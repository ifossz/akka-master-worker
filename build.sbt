import Dependencies._

lazy val commonSettings = List(
  organization := "org.ifossz",
  scalaVersion := "2.12.3",
  version      := "0.1.0-SNAPSHOT"
)

lazy val root = (project in file(".")).
  settings(
    inThisBuild(commonSettings),
    name := "Akka distributed master worker",
    libraryDependencies ++= deps
  )
