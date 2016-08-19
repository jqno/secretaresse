package nl.jqno.secretaresse

import java.awt.Toolkit.getDefaultToolkit
import java.net.URL

object Main extends App {

  val secretaresse = new Secretaresse(args.headOption getOrElse "application.conf")

  // To make the docIcon disappear http://stackoverflow.com/questions/5057639/systemtray-based-application-without-window-on-mac-os-x

  Tray().createTray(
    tooltip = "Secretaresse app",
    icon = getDefaultToolkit.getImage(new URL("https://github.com/encharm/Font-Awesome-SVG-PNG/blob/master/white/png/64/calendar-check-o.png?raw=true")),
    actions = Map(
      ("Run now", e => secretaresse.sync())
    ))
}
