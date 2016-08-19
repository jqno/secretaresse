package nl.jqno.secretaresse

import java.awt.Toolkit.getDefaultToolkit
import java.net.URL
import java.util.{Timer, TimerTask}

import scala.collection.immutable.ListMap

object Main extends App {

  // To make the docIcon disappear http://stackoverflow.com/questions/5057639/systemtray-based-application-without-window-on-mac-os-x

  val secretaresse = new Secretaresse(args.headOption getOrElse "application.conf")

  var timer = new Timer()

  Tray().createTray(
    tooltip = "Secretaresse app",

    icon = getDefaultToolkit.getImage(new URL("https://github.com/encharm/Font-Awesome-SVG-PNG/blob/master/white/png/64/calendar-check-o.png?raw=true")),

    actions = ListMap(
      ("Run now", e => secretaresse.sync()),
      ("Run every 5 minutes", e => scheduleTask(5 * 60 * 1000)),
      ("Run every 30 minutes", e => scheduleTask(30 * 60 * 1000)),
      ("Run every hour", e => scheduleTask(60 * 60 * 1000)),
      ("Stop running", e => stopTimer())
    ))

  def scheduleTask(period: Int) = {
    stopTimer()
    timer.schedule(new TimerTask {
      override def run(): Unit = secretaresse.sync()
    }, 1000, period)
  }

  def stopTimer() = {
    timer.cancel()
    timer.purge()
    timer = new Timer()
  }

  while (true)
    Thread.sleep(30000)

  timer.cancel()
}
