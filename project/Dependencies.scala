import sbt._

object Dependencies {

  object version {
    val circe = "0.12.0-RC3"
    val circeDerivation = "0.12.0-M5"
    val elastic4s = "7.3.0"
    val http4s = "0.21.0-M4"
    val log4cats = "1.0.0-RC1"
    val pureconfig = "0.11.1"
    val scalatest = "3.0.8"
    val slf4j = "1.7.28"
  }

  lazy val root = Seq(
    "io.circe" %% "circe-derivation" % version.circeDerivation,
    "com.sksamuel.elastic4s" %% "elastic4s-core" % version.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % version.elastic4s, // the default http client
    "com.sksamuel.elastic4s" %% "elastic4s-effect-cats" % version.elastic4s, // for IO (cats) as an alternative executor to Future
    "com.sksamuel.elastic4s" %% "elastic4s-json-circe" % version.elastic4s,
    "org.http4s" %% "http4s-blaze-server" % version.http4s,
    "org.http4s" %% "http4s-circe" % version.http4s,
    "org.http4s" %% "http4s-dsl" % version.http4s,
    "io.chrisdavenport" %% "log4cats-slf4j" % version.log4cats,
    "com.github.pureconfig" %% "pureconfig" % version.pureconfig,
    "org.slf4j" % "slf4j-simple" % version.slf4j,
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % version.elastic4s % "it,test",
    "io.chrisdavenport" %% "log4cats-noop" % version.log4cats % "it,test",
    "org.scalatest" %% "scalatest" % version.scalatest % "it,test"
  )
}
