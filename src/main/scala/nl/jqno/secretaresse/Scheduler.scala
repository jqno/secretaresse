package nl.jqno.secretaresse

import java.util.{Timer, TimerTask}

import scala.concurrent.duration._

class Scheduler(task: => Unit) {

  private var timer = new Timer()

  def runEvery(period: Duration) = {
    stop()
    timer.schedule(new TimerTask {
      override def run(): Unit = task
    }, 1.second.toMillis, period.toMillis)

  }

  def stop(): Unit = {
    timer.cancel()
    timer.purge()
    timer = new Timer()
  }
}

object Scheduler {
  def apply(task: => Unit): Scheduler = new Scheduler(task)
}
