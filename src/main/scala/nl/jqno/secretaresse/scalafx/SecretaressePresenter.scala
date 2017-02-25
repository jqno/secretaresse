package nl.jqno.secretaresse.scalafx

import java.awt.Toolkit._
import java.net.URL
import javax.swing.SwingUtilities

import com.typesafe.scalalogging.StrictLogging
import nl.jqno.secretaresse.{Scheduler, Secretaresse, Tray}

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scalafx.application.{JFXApp, Platform}
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.HBox
import scalafx.scene.paint.Color._
import scalafx.scene.text.Text

object SecretaressePresenter extends JFXApp with StrictLogging {

  val secretaresse = new Secretaresse("application.conf")

  val scheduler = Scheduler(run())

  def run(): Unit = {
    secretaresse.sync() onComplete {
      case Success(()) => // do nothing
      case Failure(e) => Tray().notifyUser("Synchronising failed :(")
    }
  }

  Platform.implicitExit_=(false)

  stage = new JFXApp.PrimaryStage {
    title.value = "Hello Stage"
    width = 600
    height = 450
    scene = new Scene {
      fill = rgb(236, 236, 236)
      content = new HBox {
        padding = Insets(20)
        children = Seq(
          new Text {
            text = "Preferences:"
            style = "-fx-font-size: 12pt"
          }
        )
      }
    }
  }

  SwingUtilities.invokeLater(new Runnable {
    def run() {
      Tray().createTray(
        tooltip = "Secretaresse app",

        icon = getDefaultToolkit.getImage(new URL("https://github.com/encharm/Font-Awesome-SVG-PNG/blob/master/white/png/64/calendar-check-o.png?raw=true")),

        actions = ListMap(
          ("Run now", e => run()),
          ("Run every 5 minutes", e => scheduler.runEvery(5.minutes)),
          ("Run every 30 minutes", e => scheduler.runEvery(30.minutes)),
          ("Run every hour", e => scheduler.runEvery(1.hour)),
          ("Stop running", e => scheduler.stop()),
          ("Preferences...", e => Platform.runLater(showStage()))
        ))
    }
  })

  def showStage() = {
    stage.show()
    stage.toFront()
  }
}