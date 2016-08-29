package nl.jqno.secretaresse

import java.awt.Toolkit.getDefaultToolkit
import java.net.URL

import com.typesafe.scalalogging.StrictLogging

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main extends App with StrictLogging {

  val secretaresse = new Secretaresse(args.headOption getOrElse "application.conf")

  val scheduler = Scheduler(run())

  def run(): Unit = {
    secretaresse.sync() onComplete {
      case Success(()) => // do nothing
      case Failure(e) => // show a popup or something
    }
  }

  Tray().createTray(
    tooltip = "Secretaresse app",

    icon = getDefaultToolkit.getImage(new URL("https://github.com/encharm/Font-Awesome-SVG-PNG/blob/master/white/png/64/calendar-check-o.png?raw=true")),

    actions = ListMap(
      ("Run now", e => run()),
      ("Run every 5 minutes", e => scheduler.runEvery(5.minutes)),
      ("Run every 30 minutes", e => scheduler.runEvery(30.minutes)),
      ("Run every hour", e => scheduler.runEvery(1.hour)),
      ("Stop running", e => scheduler.stop())
    ))

  while (true) {
    Thread.sleep(30000)
  }
}
