package nl.jqno.secretaresse

import java.awt._
import java.awt.event.{ActionEvent, ActionListener}
import java.lang.Runtime.getRuntime

import scala.collection.immutable.ListMap

class Tray {

  def createTray(tooltip: String, icon: Image, actions: ListMap[String, ActionEvent => Unit]): Unit = {
    val popup: PopupMenu = new PopupMenu()

    actions.foreach { case (title, action) => popup.add(createMenuItem(title, action)) }

    popup.addSeparator()
    popup.add(createMenuItem("Quit", e => System.exit(0)))

    val trayIcon = new TrayIcon(icon, tooltip, popup)
    trayIcon.setImageAutoSize(true)

    val tray = SystemTray.getSystemTray
    tray.add(trayIcon)
  }


  private def createMenuItem(title: String, action: ActionEvent => Unit): MenuItem = {
    val menuItem = new MenuItem(title)
    menuItem.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = action(e)
    })
    menuItem
  }

  def notifyUser(title: String): Unit = {
    val command = """display notification "$title" sound name "Purr" """
    getRuntime exec Array("osascript", "-e", command)
  }
}

object Tray {
  def apply() = new Tray()
}
