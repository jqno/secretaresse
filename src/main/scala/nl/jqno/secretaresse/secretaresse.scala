package nl.jqno.secretaresse

import java.io.{File, FileReader}
import java.net.URI
import java.util.{TimeZone, Date, GregorianCalendar}

import scala.collection.JavaConverters._

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.{Calendar, CalendarScopes}
import com.google.api.services.calendar.model.{Event, EventDateTime}
import com.typesafe.config.{ConfigFactory, Config}
import microsoft.exchange.webservices.data.core.{ExchangeService, PropertySet}
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion
import microsoft.exchange.webservices.data.core.enumeration.property.{BasePropertySet, BodyType, WellKnownFolderName}
import microsoft.exchange.webservices.data.core.service.folder.CalendarFolder
import microsoft.exchange.webservices.data.credential.WebCredentials
import microsoft.exchange.webservices.data.search.CalendarView

case class Appointment(
    startDate: Date, endDate: Date, subject: String, location: String,
    body: String, isAllDay: Boolean, googleId: Option[String] = None) {

  override def toString = s"${if (isAllDay) "A" else " "}($startDate - $endDate) @ $location -> $subject"

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

object Secretaresse extends App {

  def loadConfig: Config = {
    // TODO: ceedubs ficus
    val location = args.headOption getOrElse "application.conf"
    println(s"Loading $location")
    val externalConfig = ConfigFactory.parseFile(new File(location))
    ConfigFactory.load(externalConfig)
  }

  def window(daysPast: Int, daysFuture: Int): (Date, Date) = {
    val now = new GregorianCalendar()
    val start = now.clone().asInstanceOf[GregorianCalendar]
    start.add(java.util.Calendar.DATE, -daysPast)
    val end = now.clone().asInstanceOf[GregorianCalendar]
    end.add(java.util.Calendar.DATE, daysFuture)
    (start.getTime, end.getTime)
  }

  def getAppointmentsFromExchange(config: Config, from: Date, to: Date): Set[Appointment] = {
    val service = new ExchangeService(ExchangeVersion.Exchange2010_SP2)
    val credentials = new WebCredentials(config.getString("exchange.userName"), config.getString("exchange.password"))
    service.setCredentials(credentials)
    service.setUrl(new URI(config.getString("exchange.url")))
    val propertySet = new PropertySet(BasePropertySet.FirstClassProperties)
    propertySet.setRequestedBodyType(BodyType.Text)
    val cf = CalendarFolder.bind(service, WellKnownFolderName.Calendar)
    val results = cf.findAppointments(new CalendarView(from, to)).getItems.asScala

    val appointments = results map { appt =>
      appt.load(propertySet)
      val subject = Option(appt.getSubject) getOrElse ""
      val location = Option(appt.getLocation) getOrElse ""
      val body = Option(appt.getBody.toString) getOrElse ""
      val isAllDay = appt.getIsAllDayEvent
      val app = Appointment(appt.getStart, appt.getEnd, subject, location, body, isAllDay)
      app
    }
    appointments.toSet
  }

  def buildGoogleCalendarService(config: Config): Calendar = {
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

  def getCalendarId(service: Calendar, name: String): String = {
    val calendars = service.calendarList.list.execute().getItems.asScala
    calendars.find(_.getSummary == name).get.getId
  }

  def getAppointmentsFromGoogle(service: Calendar, calendarId: String, from: Date, to: Date): Set[Appointment] = {
    // list the next 10 items from the specified calendar
    val events = service.events.list(calendarId)
        .setTimeMin(new DateTime(from))
        .setTimeMax(new DateTime(to))
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .execute()
        .getItems.asScala

    val appointments = events map { event =>
      val start = Option(event.getStart.getDateTime) getOrElse event.getStart.getDate
      val end = Option(event.getEnd.getDateTime) getOrElse event.getEnd.getDate
      val summary = Option(event.getSummary) getOrElse ""
      val location = Option(event.getLocation) getOrElse ""
      val description = Option(event.getDescription) getOrElse ""
      val isAllDay = event.getStart.getDateTime == null
      val app = Appointment(new Date(start.getValue), new Date(end.getValue), summary, location, description, isAllDay, Some(event.getId))
      app
    }
    appointments.toSet
  }

  def removeAppointmentsFromGoogle(service: Calendar, calendarId: String, toRemove: Set[Appointment]): Unit = {
    toRemove foreach { appt => appt.googleId match {
      case Some(id) => service.events.delete(calendarId, id).execute()
      case _ => // do nothing
    }}
  }

  def addAppointmentsToGoogle(service: Calendar, calendarId: String, toAdd: Set[Appointment]): Unit = {
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

  def eventDate(date: Date, isAllDay: Boolean): EventDateTime =
    if (isAllDay)
      new EventDateTime().setDate(new DateTime(true, toUtc(date).getTime, 0))
    else
      new EventDateTime().setDateTime(new DateTime(date))

  def toUtc(date: Date): Date = {
    val result = new GregorianCalendar(TimeZone.getTimeZone("UTC"))
    result.set(java.util.Calendar.YEAR, date.getYear + 1900)
    result.set(java.util.Calendar.MONTH, date.getMonth)
    result.set(java.util.Calendar.DATE, date.getDate)
    result.set(java.util.Calendar.HOUR, 0)
    result.set(java.util.Calendar.MINUTE, 0)
    result.set(java.util.Calendar.SECOND, 0)
    result.set(java.util.Calendar.MILLISECOND, 0)
    result.getTime
  }

  def toRemove(source: Set[Appointment], target: Set[Appointment]): Set[Appointment] = target -- source
  def toAdd(source: Set[Appointment], target: Set[Appointment]): Set[Appointment] = source -- target

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

  run()
}

