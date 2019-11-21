package io.sherpair.w4s.geo.config

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.config.{AuthToken, Config4e, Engine, Host, Service, SSLData, Suggestions}
import pureconfig.ConfigSource
// Needed.
import pureconfig.generic.auto._
// Needed.
import pureconfig.module.enumeratum._

case class GeoConfig(
  authToken: AuthToken,
  cacheHandlerInterval: FiniteDuration,
  countries: String,
  engine: Engine,
  host: Host,
  httpPoolSize: Int,
  loaderData: LoaderData,
  plainHttp: Option[Boolean],
  root: String,
  service: Service,
  sslData: SSLData,
  suggestions: Suggestions
) extends Config4e

object GeoConfig {
  def apply(): GeoConfig = ConfigSource.default.loadOrThrow[GeoConfig]
}
