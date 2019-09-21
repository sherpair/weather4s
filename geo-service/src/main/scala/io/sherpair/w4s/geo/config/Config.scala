package io.sherpair.w4s.geo.config

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.config.{Engine, Http}
import pureconfig.ConfigSource
// Needed.
import pureconfig.generic.auto._

case class Configuration(cacheHandlerInterval: FiniteDuration, engine: Engine, httpGeo: Http, httpLoader: Http)

object Configuration {
  def apply(): Configuration = ConfigSource.default.loadOrThrow[Configuration]
}
