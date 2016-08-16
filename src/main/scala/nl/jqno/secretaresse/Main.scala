package nl.jqno.secretaresse

object Main extends App {

  val secretaresse = new Secretaresse(args.headOption getOrElse "application.conf")
  secretaresse.sync()

}
