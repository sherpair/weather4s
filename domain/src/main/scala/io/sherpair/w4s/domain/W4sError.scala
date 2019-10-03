package io.sherpair.w4s.domain

import io.sherpair.w4s.config.Configuration

case class W4sError(msg: String, cause: Option[Throwable] = None)(
  implicit C: Configuration
) extends RuntimeException(s"${C.service.name}: ${msg}", cause.orNull)
