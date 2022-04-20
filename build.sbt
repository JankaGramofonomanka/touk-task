name := """touk-task"""
organization := "com.janserwatka"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.30.0"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.janserwatka.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.janserwatka.binders._"
