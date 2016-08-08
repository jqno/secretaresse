package nl.jqno.secretaresse

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

    stage = new PrimaryStage {
      title = "Secretaresse App"
      scene = new Scene {
        content = createMenu()
      }
    }
}
