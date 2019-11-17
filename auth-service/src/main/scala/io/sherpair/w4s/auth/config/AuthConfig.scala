package io.sherpair.w4s.auth.config

import java.nio.charset.StandardCharsets.UTF_8

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
  plainHttp: Option[Boolean],
  privateKey: String,
  root: String,
  service: Service,
  sslData: SSLData,
  token: Token
) extends Configuration {

  val healthAttemptsDB: Int = db.healthCheck.attempts
  val healthIntervalDB: FiniteDuration = db.healthCheck.interval
}

object AuthConfig {

/* To keep for future reference...
   in case of optional case classes (config's properties) containing types not supported by pureconfig
     ex.  AuthConfig(postman: Option[Postman])

  implicit val postmanReader: ConfigReader[Postman] =
    (_: ConfigCursor) => Left(ConfigReaderFailures(ConvertFailure(KeyNotFound("postman"), None, path = "")))
*/

  implicit val secretReader: ConfigReader[Array[Byte]] =
    ConfigReader[String].map(_.getBytes(UTF_8))

  def apply(): AuthConfig = ConfigSource.default.loadOrThrow[AuthConfig]
}
