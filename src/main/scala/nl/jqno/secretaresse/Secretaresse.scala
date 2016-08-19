package nl.jqno.secretaresse

import java.io.File
import java.util.{Date, GregorianCalendar}

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Secretaresse(configLocation: String) {
  def sync(): Future[Unit] = {
    val config = loadConfig
    val (startDate, endDate) = window(config.getInt("app.pastDays"), config.getInt("app.futureDays"))
    val exchange = new ExchangeInterface(config)
    val google = new GoogleInterface(config)

    for {
      exchangeAppointments <- exchange.getAppointments(startDate, endDate)
      googleAppointments <- google.getAppointments(startDate, endDate)

      itemsToRemove = toRemove(exchangeAppointments, googleAppointments)
      _ <- google.removeAppointments(itemsToRemove)

      itemsToAdd = toAdd(exchangeAppointments, googleAppointments)
      _ <- google.addAppointments(itemsToAdd)
    } yield ()
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

