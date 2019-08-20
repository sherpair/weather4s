package io.sherpair.geo.domain

sealed case class GeoError(msg: String, cause: Option[Throwable]) extends RuntimeException(msg, cause.orNull)
