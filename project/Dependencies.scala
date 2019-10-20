import sbt._

object Dependencies {

  object version {
    val cats = "2.0.0"
    val catsEffect = "2.0.0"
    val circe = "0.12.2"
    val circeDerivation = "0.12.0-M7"
    val doobie = "0.8.4"
    val enumeratum = "1.5.13"
    val enumeratumCirce = "1.5.22"
    val fs2 = "2.0.1"
    val flyway = "6.0.7"
    val elastic4s = "7.3.1"
    val http4s = "0.21.0-M5"
    val log4cats = "1.0.1"
    val logback = "1.2.3"
    val lucene = "8.2.0"
    val postgres = "42.2.8"
    val pureconfig = "0.12.1"
    val scalatest = "3.0.8"
  }

  lazy val base = Seq(
    "org.typelevel" %% "cats-core" % version.cats,
    "org.typelevel" %% "cats-kernel" % version.cats,
    "org.typelevel" %% "cats-macros" % version.cats,
    "org.typelevel" %% "cats-effect" % version.catsEffect,
    "io.circe" %% "circe-core" % version.circe,
    "io.circe" %% "circe-derivation" % version.circeDerivation,
    "io.circe" %% "circe-jawn" % version.circe,
    "io.circe" %% "circe-parser" % version.circe,
    "com.beachape" %% "enumeratum" % version.enumeratum,
    "com.beachape" %% "enumeratum-circe" % version.enumeratumCirce,
    "io.chrisdavenport" %% "log4cats-slf4j" % version.log4cats,
    "ch.qos.logback" % "logback-classic" % version.logback,
    "com.github.pureconfig" %% "pureconfig" % version.pureconfig,
    "com.github.pureconfig" %% "pureconfig-enumeratum" % version.pureconfig,
    "io.chrisdavenport" %% "log4cats-noop" % version.log4cats % "test",
    "org.scalatest" %% "scalatest" % version.scalatest % "test"
  )

  lazy val doobie = Seq(
    "org.flywaydb"   %  "flyway-core"      % version.flyway,
    "org.postgresql" %  "postgresql"       % version.postgres,
    "org.tpolecat"   %% "doobie-core"      % version.doobie,
    "org.tpolecat"   %% "doobie-hikari"    % version.doobie,
    "org.tpolecat"   %% "doobie-postgres"  % version.doobie,

    "org.tpolecat"   %% "doobie-h2"        % version.doobie % "test",
    "org.tpolecat"   %% "doobie-scalatest" % version.doobie % "test"
  )

  lazy val elastic = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % version.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % version.elastic4s, // the default http client
    "com.sksamuel.elastic4s" %% "elastic4s-effect-cats" % version.elastic4s // for IO (cats) as an alternative executor to Future
    // "com.sksamuel.elastic4s" %% "elastic4s-testkit" % version.elastic4s % "test"
  )

  lazy val fs2 = Seq(
    "co.fs2" %% "fs2-core" % version.fs2,
    "co.fs2" %% "fs2-io" % version.fs2
  )

  lazy val http4s = Seq(
    "org.http4s" %% "http4s-blaze-server" % version.http4s,
    "org.http4s" %% "http4s-circe" % version.http4s,
    "org.http4s" %% "http4s-dsl" % version.http4s
  )

  lazy val http4sClient = Seq(
    "org.http4s" %% "http4s-blaze-client" % version.http4s
  )

  lazy val lucene = Seq(
    "org.apache.lucene" % "lucene-core"    % version.lucene % "test",
    "org.apache.lucene" % "lucene-suggest" % version.lucene % "test"
  )
}
