package io.sherpair.w4s.auth.config

import java.nio.charset.StandardCharsets

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.config.{AuthToken, Configuration, Host, Service, SSLData}
import pureconfig.{ConfigReader, ConfigSource}
// Needed.
import pureconfig.generic.auto._

case class AuthConfig(
  authToken: AuthToken,
  db: DB,
  host: Host,
  httpPoolSize: Int,
  plainHttp: Boolean,
  privateKey: String,
  root: String,
  service: Service,
  smtp: Host,
  sslData: SSLData,
  token: Token
) extends Configuration {

  val healthAttemptsDB: Int = db.healthCheck.attempts
  val healthIntervalDB: FiniteDuration = db.healthCheck.interval
}

object AuthConfig {

  implicit val secretReader: ConfigReader[Array[Byte]] =
    ConfigReader[String].map(_.getBytes(StandardCharsets.UTF_8))

  def apply(): AuthConfig = ConfigSource.default.loadOrThrow[AuthConfig]
}
