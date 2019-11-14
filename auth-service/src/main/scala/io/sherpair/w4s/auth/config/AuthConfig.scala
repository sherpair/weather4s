package io.sherpair.w4s.auth.config

import java.nio.charset.StandardCharsets

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.config.{AuthToken, Configuration, Host, Service, SSLData}
import pureconfig.{ConfigCursor, ConfigReader, ConfigSource}
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, KeyNotFound}
// Needed.
import pureconfig.generic.auto._

case class AuthConfig(
  authToken: AuthToken,
  db: DB,
  host: Host,
  httpPoolSize: Int,
  plainHttp: Option[Boolean],
  privateKey: String,
  root: String,
  service: Service,
  smtp: Option[Smtp],
  sslData: SSLData,
  token: Token
) extends Configuration {

  val healthAttemptsDB: Int = db.healthCheck.attempts
  val healthIntervalDB: FiniteDuration = db.healthCheck.interval
}

object AuthConfig {

  implicit val secretReader: ConfigReader[Array[Byte]] =
    ConfigReader[String].map(_.getBytes(StandardCharsets.UTF_8))

  implicit val smtpReader: ConfigReader[Smtp] =
    (_: ConfigCursor) => Left(ConfigReaderFailures(ConvertFailure(KeyNotFound("smtp"), None, path = "")))

  def apply(): AuthConfig = ConfigSource.default.loadOrThrow[AuthConfig]
}
