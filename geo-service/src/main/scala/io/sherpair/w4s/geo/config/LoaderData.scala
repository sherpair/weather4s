package io.sherpair.w4s.geo.config

import io.sherpair.w4s.config.Host

case class LoaderData(
  host: Host,
  plainHttp: Option[Boolean],
  segment: String
)
