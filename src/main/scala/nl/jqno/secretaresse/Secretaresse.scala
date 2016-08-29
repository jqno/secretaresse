package nl.jqno.secretaresse

import java.io.File
import java.util.{Date, GregorianCalendar}

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Secretaresse(configLocation: String) extends StrictLogging {

  def sync(): Future[Unit] = {
    val config = loadConfig
    val (startDate, endDate) = window(config.getInt("app.pastDays"), config.getInt("app.futureDays"))
    val exchange = new ExchangeInterface(config)
    val google = new GoogleInterface(config)

    val exchangeAppointments = exchange.getAppointments(startDate, endDate)
    val googleAppointments = google.getAppointments(startDate, endDate)

    val itemsToAdd = action(exchangeAppointments, googleAppointments, toAdd)
    val itemsToRemove = action(exchangeAppointments, googleAppointments, toRemove)

    val added = itemsToAdd.flatMap(google.addAppointments)
    val removed = itemsToRemove.flatMap(google.removeAppointments)

    for {
      _ <- added
      _ <- removed
      _ = logger.info("Done")
    } yield ()
  }

  private def loadConfig: Config = {
    logger.info(s"Loading $configLocation")
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

  private def action[A](source: Future[Set[A]], target: Future[Set[A]], f: (Set[A], Set[A]) => Set[A]): Future[Set[A]] =
    for {
      s <- source
      t <- target
    } yield f(s, t)

  def toRemove(source: Set[Appointment], target: Set[Appointment]): Set[Appointment] = target -- source
  def toAdd(source: Set[Appointment], target: Set[Appointment]): Set[Appointment] = source -- target
}

