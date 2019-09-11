import sbt._

object Dependencies {

  object version {
    val cats = "2.0.0"
    val catsEffect = "2.0.0"
    val circe = "0.12.0-RC4"
    val circeDerivation = "0.12.0-M6"
    val fs2 = "2.0.0"
    val elastic4s = "7.3.1"
    val http4s = "0.21.0-M4"
    val log4cats = "1.0.0-RC1"
    val logback = "1.2.3"
    val lucene4s = "1.9.1"
    val pureconfig = "0.11.1"
    val scalatest = "3.0.8"
  }

  lazy val root = Seq(
    "org.typelevel" %% "cats-core" % version.cats,
    "org.typelevel" %% "cats-kernel" % version.cats,
    "org.typelevel" %% "cats-macros" % version.cats,
    "org.typelevel" %% "cats-effect" % version.catsEffect,
    "io.circe" %% "circe-derivation" % version.circeDerivation,
    "com.sksamuel.elastic4s" %% "elastic4s-core" % version.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % version.elastic4s, // the default http client
    "com.sksamuel.elastic4s" %% "elastic4s-effect-cats" % version.elastic4s, // for IO (cats) as an alternative executor to Future
    // "com.sksamuel.elastic4s" %% "elastic4s-json-circe" % version.elastic4s,
    "co.fs2" %% "fs2-io" % version.fs2,
    "org.http4s" %% "http4s-blaze-server" % version.http4s,
    "org.http4s" %% "http4s-circe" % version.http4s,
    "org.http4s" %% "http4s-dsl" % version.http4s,
    "io.chrisdavenport" %% "log4cats-slf4j" % version.log4cats,
    "ch.qos.logback" % "logback-classic" % version.logback,
    "com.github.pureconfig" %% "pureconfig" % version.pureconfig,
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % version.elastic4s % "it,test",
    "io.chrisdavenport" %% "log4cats-noop" % version.log4cats % "it,test",
    "com.outr" %% "lucene4s" % version.lucene4s % "test",
    "org.scalatest" %% "scalatest" % version.scalatest % "it,test"
  )
}
