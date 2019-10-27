package io.sherpair.w4s.geo.config

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.config.{Host, Service}
import io.sherpair.w4s.config.{Config4e, Engine, Suggestions}
import pureconfig.ConfigSource
// Needed.
import pureconfig.generic.auto._
// Needed.
import pureconfig.module.enumeratum._

case class SSLGeo(
  algorithm: String, host: Host, keyStore: String, password: String, randomAlgorithm: String, `type`: String
)

case class GeoConfig(
  cacheHandlerInterval: FiniteDuration,
  countries: String,
  engine: Engine,
  hostAuth: Host,
  hostGeo: Host,
  hostLoader: Host,
  httpPoolSize: Int,
  service: Service,
  sslGeo: SSLGeo,
  suggestions: Suggestions
) extends Config4e

object GeoConfig {
  def apply(): GeoConfig = ConfigSource.default.loadOrThrow[GeoConfig]
}
