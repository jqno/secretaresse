package nl.jqno.secretaresse

import java.awt.{Image, PopupMenu, Toolkit, TrayIcon}
import java.awt.{SystemTray, MenuItem}
import java.awt.event.{ActionEvent, ActionListener}
import java.net.URL

object MainTrayApp {

  def main(args: Array[String]): Unit = {

    val popup: PopupMenu = new PopupMenu()
    popup.add(createMenuItem("Run now", e => print("Run now")))
    popup.add(createMenuItem("Schedule every 5 minutes", e => print("Schedule every 5 minutes")))
    popup.add(createMenuItem("Schedule every 30 minutes", e => print("Schedule every 30 minutes")))
    popup.add(createMenuItem("Turn off", e => print("Turn off")))
    popup.addSeparator()
    popup.add(createMenuItem("Quit", e => System.exit(0)))

    val image: Image = Toolkit.getDefaultToolkit.getImage(new URL("http://www.dijklandfm.nl/wp-content/uploads/test.png"))
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
