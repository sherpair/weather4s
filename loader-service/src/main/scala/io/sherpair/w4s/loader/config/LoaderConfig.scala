package io.sherpair.w4s.loader.config

import io.sherpair.w4s.config.{Configuration, Engine, Http, Service, Suggestions}
import pureconfig.ConfigSource
// Needed.
import pureconfig.module.enumeratum._
// Needed.
import pureconfig.generic.auto._

case class LoaderConfig(
  countryDownloadUrl: String,
  engine: Engine,
  httpLoader: Http,
  httpPoolSize: Int,
  maxEnqueuedCountries: Int,
  service: Service,
  suggestions: Suggestions
) extends Configuration

object LoaderConfig {
  def apply(): LoaderConfig = ConfigSource.default.loadOrThrow[LoaderConfig]
}
