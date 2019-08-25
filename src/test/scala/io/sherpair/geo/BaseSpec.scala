package io.sherpair.geo

import cats.Applicative
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.noop.NoOpLogger

trait BaseSpec {
  implicit def logger[F[_]: Applicative]: Logger[F] = NoOpLogger.impl[F]
}
