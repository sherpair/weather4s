package io.sherpair.geo.domain

sealed abstract class Error(msg: String, cause: Option[Throwable] = None) extends RuntimeException(msg, cause.orNull)

case class GeoError(msg: String) extends Error(msg)
