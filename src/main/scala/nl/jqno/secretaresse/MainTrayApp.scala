package nl.jqno.secretaresse

import java.awt.{Image, PopupMenu, Toolkit, TrayIcon}
import java.awt.{SystemTray, MenuItem}
import java.awt.event.{ActionEvent, ActionListener}
import java.net.URL

object MainTrayApp {

  //In order to not show a dock item we need to use this I think: http://stackoverflow.com/questions/5057639/systemtray-based-application-without-window-on-mac-os-x

  val secretaresse: Secretaresse = new Secretaresse

  def main(args: Array[String]): Unit = {
    val popup: PopupMenu = new PopupMenu()
    popup.add(createMenuItem("Run now", e => secretaresse.run()))
    popup.addSeparator()
    popup.add(createMenuItem("Quit", e => System.exit(0)))

    val image: Image = Toolkit.getDefaultToolkit.getImage(new URL("https://github.com/encharm/Font-Awesome-SVG-PNG/blob/master/white/png/64/calendar-check-o.png?raw=true"))
    val trayIcon: TrayIcon = new TrayIcon(image, "Secretaresse app", popup)
    trayIcon.setImageAutoSize(true)

    val tray: SystemTray = SystemTray.getSystemTray
    tray.add(trayIcon)
  }

  def createMenuItem(title: String, action: ActionEvent => Unit): MenuItem = {
    val menuItem = new MenuItem(title)

    menuItem.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = action(e)
    })

    menuItem
  }
}
