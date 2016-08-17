package nl.jqno.secretaresse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

object Util {
  def async[T](block: => T): Future[T] = Future {
    blocking {
      block
    }
  }
}
