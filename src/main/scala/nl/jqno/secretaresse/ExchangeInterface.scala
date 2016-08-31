package nl.jqno.secretaresse

import java.net.URI
import java.util.Date

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion
import microsoft.exchange.webservices.data.core.enumeration.property.{BasePropertySet, BodyType, WellKnownFolderName}
import microsoft.exchange.webservices.data.core.service.folder.CalendarFolder
import microsoft.exchange.webservices.data.core.service.item.{Appointment => ExchangeAppointment}
import microsoft.exchange.webservices.data.core.{ExchangeService, PropertySet}
import microsoft.exchange.webservices.data.credential.WebCredentials
import microsoft.exchange.webservices.data.search.CalendarView

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}

class ExchangeInterface(config: Config) extends StrictLogging {

  def getAppointments(from: Date, to: Date): Future[Set[Appointment]] = Future {
    logger.info("Getting events from Exchange...")

    val service = new ExchangeService(ExchangeVersion.Exchange2010_SP2)
    val credentials = new WebCredentials(config.getString("exchange.userName"), config.getString("exchange.password"))
    service.setCredentials(credentials)
    service.setUrl(new URI(config.getString("exchange.url")))
    val propertySet = new PropertySet(BasePropertySet.FirstClassProperties)
    propertySet.setRequestedBodyType(BodyType.Text)

    val cf = blocking { CalendarFolder.bind(service, WellKnownFolderName.Calendar) }
    val appointments = blocking { cf.findAppointments(new CalendarView(from, to)).getItems.asScala.toSet }
    appointments.map(convert(propertySet, _))
  }

  private def convert(propertySet: PropertySet, appt: ExchangeAppointment): Appointment = {
    blocking { appt.load(propertySet) }
    val subject = Option(appt.getSubject) getOrElse ""
    val location = Option(appt.getLocation) getOrElse ""
    val body = Option(appt.getBody.toString) getOrElse ""
    val isAllDay = appt.getIsAllDayEvent
    Appointment(appt.getStart, appt.getEnd, subject, location, body, isAllDay)
  }
}
