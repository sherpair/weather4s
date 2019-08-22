package io.sherpair.geo.domain

sealed case class GeoError(msg: String, cause: Option[Throwable] = None) extends RuntimeException(msg, cause.orNull)
