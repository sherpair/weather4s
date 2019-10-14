package io.sherpair.w4s.auth.config

import java.nio.charset.StandardCharsets

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.config.{Configuration, Engine, Http, Service, Suggestions}
import pureconfig.{ConfigReader, ConfigSource}
// Needed.
import pureconfig.generic.auto._
// Needed.
import pureconfig.module.enumeratum._

case class AuthConfig(
  db: DB,
  engine: Engine,
  httpAuth: Http,
  httpPoolSize: Int,
  service: Service,
  suggestions: Suggestions
) extends Configuration {

  val healthAttemptsDB: Int = db.healthCheck.attempts
  val healthIntervalDB: FiniteDuration = db.healthCheck.interval
}

object AuthConfig {

  implicit val passwordReader: ConfigReader[Array[Byte]] =
    ConfigReader[String].map(_.getBytes(StandardCharsets.UTF_8))

  def apply(): AuthConfig = ConfigSource.default.loadOrThrow[AuthConfig]
}
