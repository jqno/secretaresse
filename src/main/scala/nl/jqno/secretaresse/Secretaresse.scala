package nl.jqno.secretaresse

import java.io.File
import java.text.SimpleDateFormat
import java.util.{Date, GregorianCalendar, TimeZone}

import com.typesafe.config.{Config, ConfigFactory}

class Secretaresse(configLocation: String) {
  def sync(): Unit = {
    val config = loadConfig
    val (startDate, endDate) = window(config.getInt("app.pastDays"), config.getInt("app.futureDays"))

    println("Getting events from Exchange...")
    val exchange = new ExchangeInterface(config)
    val exchangeAppointments = exchange.getAppointments(startDate, endDate)

    println("Connecting to Google...")
    val google = new GoogleInterface(config)
    val calendarId = google.getCalendarId(config.getString("google.calendarName"))
    println("Getting events from Google...")
    val googleAppointments = google.getAppointments(calendarId, startDate, endDate)

    val itemsToRemove = toRemove(exchangeAppointments, googleAppointments)
    println(s"Removing ${itemsToRemove.size} events from Google...")
    itemsToRemove foreach println
    google.removeAppointments(calendarId, itemsToRemove)

    val itemsToAdd = toAdd(exchangeAppointments, googleAppointments)
    println(s"Adding ${itemsToAdd.size} events to Google...")
    itemsToAdd foreach println
    google.addAppointments(calendarId, itemsToAdd)
  }

  private def loadConfig: Config = {
    // TODO: ceedubs ficus
    println(s"Loading $configLocation")
    val externalConfig = ConfigFactory.parseFile(new File(configLocation))
    ConfigFactory.load(externalConfig)
  }

  private def window(daysPast: Int, daysFuture: Int): (Date, Date) = {
    val now = new GregorianCalendar()
    val start = now.clone().asInstanceOf[GregorianCalendar]
    start.add(java.util.Calendar.DATE, -daysPast)
    val end = now.clone().asInstanceOf[GregorianCalendar]
    end.add(java.util.Calendar.DATE, daysFuture)
    (start.getTime, end.getTime)
  }

  def toRemove(source: Set[Appointment], target: Set[Appointment]): Set[Appointment] = target -- source
  def toAdd(source: Set[Appointment], target: Set[Appointment]): Set[Appointment] = source -- target
}

