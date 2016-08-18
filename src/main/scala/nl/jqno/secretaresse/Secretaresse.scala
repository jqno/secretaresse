package nl.jqno.secretaresse

import java.io.File
import java.util.{Date, GregorianCalendar}

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Await
import scala.concurrent.duration._

class Secretaresse(configLocation: String) {
  def sync(): Unit = {
    val config = loadConfig
    val (startDate, endDate) = window(config.getInt("app.pastDays"), config.getInt("app.futureDays"))

    println("Getting events from Exchange...")
    val exchange = new ExchangeInterface(config)
    val exchangeAppointments = Await.result(exchange.getAppointments(startDate, endDate), 60.seconds)

    println("Connecting to Google...")
    val google = new GoogleInterface(config)
    val calendarId = Await.result(google.getCalendarId(config.getString("google.calendarName")), 60.seconds)
    println("Getting events from Google...")
    val googleAppointments = Await.result(google.getAppointments(calendarId, startDate, endDate), 60.seconds)

    val itemsToRemove = toRemove(exchangeAppointments, googleAppointments)
    println(s"Removing ${itemsToRemove.size} events from Google...")
    itemsToRemove foreach println
    Await.result(google.removeAppointments(calendarId, itemsToRemove), 60.seconds)

    val itemsToAdd = toAdd(exchangeAppointments, googleAppointments)
    println(s"Adding ${itemsToAdd.size} events to Google...")
    itemsToAdd foreach println
    Await.result(google.addAppointments(calendarId, itemsToAdd), 60.seconds)
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

