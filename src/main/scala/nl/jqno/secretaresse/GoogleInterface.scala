package nl.jqno.secretaresse

import java.io.{File, FileReader}
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.model.{Event, EventDateTime}
import com.google.api.services.calendar.{Calendar, CalendarScopes}
import com.typesafe.config.Config
import nl.jqno.secretaresse.Util._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GoogleInterface(config: Config) {

  private lazy val service = buildGoogleCalendarService()

  private def buildGoogleCalendarService(): Calendar = {
    // global initializations
    val jsonFactory = JacksonFactory.getDefaultInstance
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport
    val scopes = List(CalendarScopes.CALENDAR).asJava
    val dataStoreDir = new File(config.getString("google.dataStoreDir"))
    val dataStoreFactory = new FileDataStoreFactory(dataStoreDir)

    // load secrets
    val in = new FileReader(config.getString("google.secrets"))
    val secrets = GoogleClientSecrets.load(jsonFactory, in)

    // build flow and trigger authorization request
    val flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, secrets, scopes)
        .setDataStoreFactory(dataStoreFactory)
        .setAccessType("offline")
        .build
    val credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver).authorize("user")

    // build an authorized Google Calendar service
    new Calendar.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName("Secretaresse")
        .build
  }

  def getCalendarId(name: String): Future[String] = async {
    val calendars = service.calendarList.list.execute().getItems.asScala
    calendars.find(_.getSummary == name).get.getId
  }

  def getAppointments(calendarId: String, from: Date, to: Date): Future[Set[Appointment]] = {
    // list the next 10 items from the specified calendar
    val events = async {
      service.events.list(calendarId)
        .setTimeMin(new DateTime(from))
        .setTimeMax(new DateTime(to))
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .execute()
        .getItems.asScala.toSet
    }

    for {
      e <- events
    } yield e.map(convert)
  }

  private def convert(event: Event): Appointment = {
    val start = getGoogleDate(event.getStart)
    val end = getGoogleDate(event.getEnd)
    val summary = Option(event.getSummary) getOrElse ""
    val location = Option(event.getLocation) getOrElse ""
    val description = Option(event.getDescription) getOrElse ""
    val isAllDay = event.getStart.getDateTime == null
    Appointment(new Date(start.getValue), new Date(end.getValue), summary, location, description, isAllDay, Some(event.getId))
  }

  private def getGoogleDate(edt: EventDateTime): DateTime = {
    Option(edt.getDateTime) match {
      case Some(dt) => dt
      case None =>
        val millis = edt.getDate.getValue
        val offset = TimeZone.getDefault.getOffset(millis)
        val unTimezoned = millis - offset
        new DateTime(new Date(unTimezoned), TimeZone.getDefault)
    }
  }

  def removeAppointments(calendarId: String, toRemove: Set[Appointment]): Future[Unit] = {
    val ids = for {
      appt <- toRemove
      id <- appt.googleId
    } yield id

    executeInParallel(ids) { id =>
      service.events.delete(calendarId, id).execute()
    }
  }

  def addAppointments(calendarId: String, toAdd: Set[Appointment]): Future[Unit] = {
    val events = toAdd map { appt =>
      new Event()
        .setStart(eventDate(appt.startDate, appt.isAllDay))
        .setEnd(eventDate(appt.endDate, appt.isAllDay))
        .setSummary(appt.subject)
        .setLocation(appt.location)
        .setDescription(appt.body)
    }

    executeInParallel(events) { event =>
      service.events.insert(calendarId, event).execute()
    }
  }

  private def eventDate(date: Date, isAllDay: Boolean): EventDateTime =
    if (isAllDay) {
      val format = new SimpleDateFormat("yyyy-MM-dd")
      val string = format.format(date)
      val dt = new DateTime(string)
      new EventDateTime().setDate(dt)
    } else {
      new EventDateTime().setDateTime(new DateTime(date))
    }
}
