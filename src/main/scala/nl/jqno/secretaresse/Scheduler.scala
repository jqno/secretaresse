package nl.jqno.secretaresse

import java.util.{Timer, TimerTask}

class Scheduler(task: Unit => Unit) {

  private var timer = new Timer()

  def schedule(period: Int) = {
    stop()
    timer.schedule(new TimerTask {
      override def run(): Unit = task()
    }, 1000, period)

  }

  def stop(): Unit = {
    timer.cancel()
    timer.purge()
    timer = new Timer()
  }
}

object Scheduler {
  def apply(task: Unit => Unit): Scheduler = new Scheduler(task)
}