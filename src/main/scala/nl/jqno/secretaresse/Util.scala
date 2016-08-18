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
    def unit[A](a: A, b: A): Unit = ()

    val tasks = xs map { x =>
      async { f(x) }
    }

    if (tasks.isEmpty)
      Future.successful(())
    else
      Future.reduce(tasks)(unit)
  }
}
