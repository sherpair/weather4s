package io.sherpair.geo

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import cats.syntax.apply._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.algebra.Engine
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.infrastructure.EngineCriticalOps
import org.http4s.server.Server

class Resources[F[_]: ConcurrentEffect: ContextShift: Engine: Logger: Timer](implicit C: Configuration) {

  def describe: Resource[F, (Unit, Server[F])] = {
    val engineCriticalOps = new EngineCriticalOps[F]

    val res1 = Resource.make(engineCriticalOps.init)(_ => engineCriticalOps.close)
    val res2 = GeoServer.describe[F]

    (res1, res2).tupled
  }
}
