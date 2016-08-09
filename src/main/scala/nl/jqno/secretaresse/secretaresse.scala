package nl.jqno.secretaresse

import java.io.{File, FileReader}
import java.net.URI
import java.text.SimpleDateFormat
import java.time.{LocalDate, ZoneId}
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
import com.typesafe.config.{Config, ConfigFactory}
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion
import microsoft.exchange.webservices.data.core.enumeration.property.{BasePropertySet, BodyType, WellKnownFolderName}
import microsoft.exchange.webservices.data.core.service.folder.CalendarFolder
import microsoft.exchange.webservices.data.core.{ExchangeService, PropertySet}
import microsoft.exchange.webservices.data.credential.WebCredentials
import microsoft.exchange.webservices.data.search.CalendarView

import scala.collection.JavaConverters._

case class Appointment(
    startDate: Date, endDate: Date, subject: String, location: String,
    body: String, isAllDay: Boolean, googleId: Option[String] = None) {

  override def toString = s"${if (googleId.isDefined) "GOOGLE" else "EXCHNG"}  ${if (isAllDay) "A" else " "}($startDate - $endDate) @ $location -> $subject"

  override def equals(obj: Any) = obj match {
    case other: Appointment =>
      startDate == other.startDate && endDate == other.endDate && subject == other.subject && location == other.location && body == other.body
    case _ =>
      false
  }

  override def hashCode = {
    val prime = 59
    var result = prime
    result = prime * result + startDate.##
    result = prime * result + endDate.##
    result = prime * result + subject.##
    result = prime * result + location.##
    result = prime * result + body.##
    result
  }
}

class Secretaresse {

  private def loadConfig: Config = {
    // TODO: ceedubs ficus
    val location = "application.conf"
    println(s"Loading $location")
    val externalConfig = ConfigFactory.parseFile(new File(location))
    ConfigFactory.load(externalConfig)
  }

  private def window(daysPast: Int, daysFuture: Int): (LocalDate, LocalDate) = {
    val start: LocalDate = LocalDate.now().minusDays(daysPast)
    val end: LocalDate = LocalDate.now().plusDays(daysFuture)

    (start, end)
  }

  private def localDateToDate(ld: LocalDate): Date = Date.from(ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant)

  private def getAppointmentsFromExchange(config: Config, from: LocalDate, to: LocalDate): Set[Appointment] = {
    val service = new ExchangeService(ExchangeVersion.Exchange2010_SP2)
    val credentials = new WebCredentials(config.getString("exchange.userName"), config.getString("exchange.password"))
    service.setCredentials(credentials)
    service.setUrl(new URI(config.getString("exchange.url")))
    val propertySet = new PropertySet(BasePropertySet.FirstClassProperties)
    propertySet.setRequestedBodyType(BodyType.Text)
    val cf = CalendarFolder.bind(service, WellKnownFolderName.Calendar)
    val results = cf.findAppointments(new CalendarView(localDateToDate(from), localDateToDate(to))).getItems.asScala

    val appointments = results map { appt =>
      appt.load(propertySet)
      val subject = Option(appt.getSubject) getOrElse ""
      val location = Option(appt.getLocation) getOrElse ""
      val body = Option(appt.getBody.toString) getOrElse ""
      val isAllDay = appt.getIsAllDayEvent
      Appointment(appt.getStart, appt.getEnd, subject, location, body, isAllDay)
    }
    appointments.toSet
  }

  private def buildGoogleCalendarService(config: Config): Calendar = {
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

  private def getCalendarId(service: Calendar, name: String): String = {
    val calendars = service.calendarList.list.execute().getItems.asScala
    calendars.find(_.getSummary == name).get.getId
  }

  private def getAppointmentsFromGoogle(service: Calendar, calendarId: String, from: LocalDate, to: LocalDate): Set[Appointment] = {
    // list the next 10 items from the specified calendar
    val events = service.events.list(calendarId)
        .setTimeMin(new DateTime(localDateToDate(from)))
        .setTimeMax(new DateTime(localDateToDate(to)))
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .execute()
        .getItems.asScala

    val appointments = events map { event =>
      val start = getGoogleDate(event.getStart)
      val end = getGoogleDate(event.getEnd)
      val summary = Option(event.getSummary) getOrElse ""
      val location = Option(event.getLocation) getOrElse ""
      val description = Option(event.getDescription) getOrElse ""
      val isAllDay = event.getStart.getDateTime == null
      Appointment(new Date(start.getValue), new Date(end.getValue), summary, location, description, isAllDay, Some(event.getId))
    }
    appointments.toSet
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

  private def removeAppointmentsFromGoogle(service: Calendar, calendarId: String, toRemove: Set[Appointment]): Unit = {
    toRemove foreach { appt => appt.googleId match {
      case Some(id) => service.events.delete(calendarId, id).execute()
      case _ => // do nothing
    }}
  }

  private def addAppointmentsToGoogle(service: Calendar, calendarId: String, toAdd: Set[Appointment]): Unit = {
    val events = toAdd map { appt =>
      new Event()
        .setStart(eventDate(appt.startDate, appt.isAllDay))
        .setEnd(eventDate(appt.endDate, appt.isAllDay))
        .setSummary(appt.subject)
        .setLocation(appt.location)
        .setDescription(appt.body)
    }
    events foreach { e =>
      service.events.insert(calendarId, e).execute()
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

  private def toRemove(source: Set[Appointment], target: Set[Appointment]): Set[Appointment] = target -- source
  private def toAdd(source: Set[Appointment], target: Set[Appointment]): Set[Appointment] = source -- target

  def run(): Unit = {
    val config = loadConfig

    val (startDate, endDate) = window(config.getInt("app.pastDays"), config.getInt("app.futureDays"))

    println("Getting events from Exchange...")
    val exchange = getAppointmentsFromExchange(config, startDate, endDate)

    println("Connecting to Google...")
    val calendarService = buildGoogleCalendarService(config)
    val calendarId = getCalendarId(calendarService, config.getString("google.calendarName"))
    println("Getting events from Google...")
    val google = getAppointmentsFromGoogle(calendarService, calendarId, startDate, endDate)

    val itemsToRemove = toRemove(exchange, google)
    println(s"Removing ${itemsToRemove.size} events from Google...")
    itemsToRemove foreach println
    removeAppointmentsFromGoogle(calendarService, calendarId, itemsToRemove)

    val itemsToAdd = toAdd(exchange, google)
    println(s"Adding ${itemsToAdd.size} events to Google...")
    itemsToAdd foreach println
    addAppointmentsToGoogle(calendarService, calendarId, itemsToAdd)
  }
}

