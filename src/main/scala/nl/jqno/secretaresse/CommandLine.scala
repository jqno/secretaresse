package nl.jqno.secretaresse

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object CommandLine extends App {
  val secretaresse = new Secretaresse(args.headOption getOrElse "application.conf")
  val f = secretaresse.sync() andThen {
    case Success(()) =>
      println("success")
    case Failure(e) =>
      println("failure")
      e.printStackTrace()
  }

  Await.result(f, 10.seconds)
}
