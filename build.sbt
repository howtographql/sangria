name := "howtograph-sangria"

version := "1.0"

description := "GraphQL server with akka-http and sangria"

scalaVersion := "2.12.3"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.sangria-graphql" %% "sangria" % "1.3.0",
  "org.sangria-graphql" %% "sangria-spray-json" % "1.0.0",
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10",

  "com.typesafe.slick" %% "slick" % "3.2.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
  "org.slf4j" % "slf4j-nop" % "1.6.6",
  "com.h2database" % "h2" % "1.4.196",

  "org.scalatest" %% "scalatest" % "3.0.4" % Test
)

Revolver.settings
