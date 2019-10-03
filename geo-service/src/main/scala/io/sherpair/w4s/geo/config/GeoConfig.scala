package io.sherpair.w4s.geo.config

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.config.{Configuration, Engine, Http, Service}
import pureconfig.ConfigSource
// Needed.
import pureconfig.generic.auto._

case class GeoConfig(
  cacheHandlerInterval: FiniteDuration,
  engine: Engine,
  httpGeo: Http,
  httpLoader: Http,
  service: Service
) extends Configuration

object GeoConfig {
  def apply(): GeoConfig = ConfigSource.default.loadOrThrow[GeoConfig]
}
