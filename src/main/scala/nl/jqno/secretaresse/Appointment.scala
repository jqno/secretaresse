package nl.jqno.secretaresse

import java.util.Date

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
