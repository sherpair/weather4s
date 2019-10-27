package io.sherpair.w4s.auth.config

import java.nio.charset.StandardCharsets

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.config.{Configuration, Host, Service}
import pureconfig.{ConfigReader, ConfigSource}
// Needed.
import pureconfig.generic.auto._

case class AuthConfig(
  db: DB,
  hostAuth: Host,
  httpPoolSize: Int,
  service: Service
) extends Configuration {

  val healthAttemptsDB: Int = db.healthCheck.attempts
  val healthIntervalDB: FiniteDuration = db.healthCheck.interval
}

object AuthConfig {

  implicit val passwordReader: ConfigReader[Array[Byte]] =
    ConfigReader[String].map(_.getBytes(StandardCharsets.UTF_8))

  def apply(): AuthConfig = ConfigSource.default.loadOrThrow[AuthConfig]
}
