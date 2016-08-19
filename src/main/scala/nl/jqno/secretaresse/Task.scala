package nl.jqno.secretaresse

import java.util.{Timer, TimerTask}

import scala.concurrent.duration._

class Task(supplier: => Unit) {

  private var timer = new Timer()

  def runEvery(period: Duration) = {
    stop()
    timer.schedule(new TimerTask {
      override def run(): Unit = supplier
    }, 1.second.toMillis, period.toMillis)

  }

  def stop(): Unit = {
    timer.cancel()
    timer.purge()
    timer = new Timer()
  }
}

object Task {
  def apply(task: => Unit): Task = new Task(task)
}
