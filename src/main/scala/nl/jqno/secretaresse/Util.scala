package nl.jqno.secretaresse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

object Util {
  def async[T](block: => T): Future[T] = Future {
    blocking {
      block
    }
  }

  def executeInParallel[T](xs: TraversableOnce[T])(f: T => Unit): Future[Unit] = {
    def unit[T](a: T, b: T): Unit = ()

    val tasks = xs map { x =>
      async { f(x) }
    }
    Future.reduce(tasks)(unit)
  }
}
