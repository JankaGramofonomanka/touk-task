name := """touk-task"""
organization := "com.janserwatka"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.15"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.0" % Test
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.30.0"
libraryDependencies += "org.typelevel" %% "cats-core" % "2.3.0"
libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.20.13-play27"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.janserwatka.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.janserwatka.binders._"
