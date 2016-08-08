name := "exchange"
organization := "nl.jqno.exchange"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"
scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "com.microsoft.ews-java-api" % "ews-java-api" % "2.0",
  "com.google.api-client" % "google-api-client" % "1.22.0",
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.22.0",
  "com.google.apis" % "google-api-services-calendar" % "v3-rev202-1.22.0",
  "com.typesafe" % "config" % "1.3.0",
  "org.scalafx" % "scalafx_2.11" % "8.0.92-R10"
)

