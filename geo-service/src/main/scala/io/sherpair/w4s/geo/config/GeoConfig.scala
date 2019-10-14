package io.sherpair.w4s.geo.config

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.config.{Configuration, Engine, Http, Service, Suggestions}
import pureconfig.ConfigSource
// Needed.
import pureconfig.module.enumeratum._
// Needed.
import pureconfig.generic.auto._

case class GeoConfig(
  cacheHandlerInterval: FiniteDuration,
  engine: Engine,
  httpPoolSize: Int,
  httpAuth: Http,
  httpGeo: Http,
  httpLoader: Http,
  service: Service,
  suggestions: Suggestions
) extends Configuration

object GeoConfig {
  def apply(): GeoConfig = ConfigSource.default.loadOrThrow[GeoConfig]
}
