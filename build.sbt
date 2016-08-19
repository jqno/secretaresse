name := "Secretaresse"
organization := "nl.jqno.exchange"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"
scalacOptions += "-deprecation"
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "com.microsoft.ews-java-api" % "ews-java-api" % "2.0",
  "com.google.api-client" % "google-api-client" % "1.22.0",
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.22.0",
  "com.google.apis" % "google-api-services-calendar" % "v3-rev202-1.22.0",
  "com.typesafe" % "config" % "1.3.0"
)

Seq(appbundle.settings: _*)
appbundle.name := "Secretaresse"
appbundle.javaVersion := "1.8+"
appbundle.icon := Some(file("Secretaresse.png"))
appbundle.workingDirectory := Some(file(appbundle.BundleVar_AppPackage))

// To make the docIcon disappear http://stackoverflow.com/questions/5057639/systemtray-based-application-without-window-on-mac-os-x
