package io.sherpair.w4s.loader.config

import io.sherpair.w4s.config.{AuthToken, Config4e, Engine, Host, Service, SSLData, Suggestions}
import pureconfig.ConfigSource
// Needed.
import pureconfig.generic.auto._
// Needed.
import pureconfig.module.enumeratum._

case class LoaderConfig(
  authToken: AuthToken,
  countryDownloadUrl: String,
  engine: Engine,
  host: Host,
  httpPoolSize: Int,
  maxEnqueuedCountries: Int,
  plainHttp: Option[Boolean],
  root: String,
  service: Service,
  sslData: SSLData,
  suggestions: Suggestions
) extends Config4e

object LoaderConfig {
  def apply(): LoaderConfig = ConfigSource.default.loadOrThrow[LoaderConfig]
}
