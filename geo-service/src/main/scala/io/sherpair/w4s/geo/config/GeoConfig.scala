package io.sherpair.w4s.geo.config

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.config.{Configuration, Engine, Host, Service, Suggestions}
import pureconfig.ConfigSource
// Needed.
import pureconfig.module.enumeratum._
// Needed.
import pureconfig.generic.auto._

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
) extends Configuration

object GeoConfig {
  def apply(): GeoConfig = ConfigSource.default.loadOrThrow[GeoConfig]
}
