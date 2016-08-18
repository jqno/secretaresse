package nl.jqno.secretaresse

import java.awt.Toolkit.getDefaultToolkit
import java.net.URL

import scala.collection.immutable.ListMap
import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

  val secretaresse = new Secretaresse(args.headOption getOrElse "application.conf")

  val scheduler = Scheduler(run())

  def run(): Unit = {
    val fut = secretaresse.sync()
    Await.result(fut, 120.seconds) //OMG
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
