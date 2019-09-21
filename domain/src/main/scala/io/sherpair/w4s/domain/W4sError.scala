package io.sherpair.w4s.domain

import io.sherpair.w4s.config.Service

case class W4sError(msg: String, cause: Option[Throwable] = None)(
  implicit service: Service
) extends RuntimeException(s"${service.name}: ${msg}", cause.orNull)
