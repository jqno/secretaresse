package nl.jqno.secretaresse

import java.awt.Toolkit.getDefaultToolkit
import java.net.URL

import scala.collection.immutable.ListMap
import scala.concurrent.duration._

object Main extends App {

  val secretaresse = new Secretaresse(args.headOption getOrElse "application.conf")

  val task = Task(secretaresse.sync())

  Tray().createTray(
    tooltip = "Secretaresse app",

    icon = getDefaultToolkit.getImage(new URL("https://github.com/encharm/Font-Awesome-SVG-PNG/blob/master/white/png/64/calendar-check-o.png?raw=true")),

    actions = ListMap(
      ("Run now", e => secretaresse.sync()),
      ("Run every 5 minutes", e => task.runEvery(5.minutes)),
      ("Run every 30 minutes", e => task.runEvery(30.minutes)),
      ("Run every hour", e => task.runEvery(1.hour)),
      ("Stop running", e => task.stop())
    ))

  while (true) {
    Thread.sleep(30000)
  }

}
