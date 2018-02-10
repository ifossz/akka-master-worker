import sbt._

object Dependencies {
  lazy val akkaVersion = "2.5.9"
  lazy val akka = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  lazy val akkaTest = "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3"
  lazy val deps = Seq(akka, scalaTest, akkaTest)
}
