package nl.jqno.secretaresse

import java.net.URL

import dorkbox.systemTray.SystemTray

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.{Menu, MenuBar, MenuItem}
import scalafx.scene.layout.BorderPane

object ScalaFxHelloWorld extends JFXApp {

    // Use this to keep the app running after the main window is closed
    // Platform.implicitExit_=(false)

    def createMenu(): BorderPane = {
      val menu = new Menu("Secretaresse") {
        items = List(
          new MenuItem("Run now"),
          new MenuItem("Schedule every 5 minutes"),
          new MenuItem("Schedule every 30 minutes"),
          new MenuItem("Turn off")
        )
      }

      new BorderPane {
        top = new MenuBar {
          menus = List(menu)
          useSystemMenuBar = true
        }
      }
    }

  def systemTray() = {
    val systemTray = SystemTray.getSystemTray
    if (systemTray == null) {
      throw new RuntimeException("Unable to load SystemTray!")
    }
    systemTray.setIcon(new URL("http://www.dijklandfm.nl/wp-content/uploads/test.png"))
    systemTray.setStatus("Not Running")
  }

    stage = new PrimaryStage {
      title = "Secretaresse App"
      scene = new Scene {
        content = createMenu()
      }
    }

  systemTray()
}
