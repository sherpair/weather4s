package io.sherpair.w4s.loader.config

import io.sherpair.w4s.config.{Config4e, Engine, Host, Service, Suggestions}
import pureconfig.ConfigSource
// Needed.
import pureconfig.generic.auto._
// Needed.
import pureconfig.module.enumeratum._

case class LoaderConfig(
  countryDownloadUrl: String,
  engine: Engine,
  hostLoader: Host,
  httpPoolSize: Int,
  maxEnqueuedCountries: Int,
  service: Service,
  suggestions: Suggestions
) extends Config4e

object LoaderConfig {
  def apply(): LoaderConfig = ConfigSource.default.loadOrThrow[LoaderConfig]
}
