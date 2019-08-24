package io.sherpair.geo.engine

import cats.effect.Sync
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.cache.Cache
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.config.Configuration._

class EngineOps[F[_]](implicit config: Configuration, engine: Engine[F], L: Logger[F], S: Sync[F]) {
  def init: F[Cache] =
    for {
      status <- engine.init
      _ <- logEngineStatus(status)
      cache <- engine.execUnderGlobalLock[Cache](createIndexesIfNotExist)
    } yield cache

  def close: F[Unit] =
    L.info(s"Closing connection with ES cluster(${clusterName(config)})") *> engine.close

  private def createIndexesIfNotExist: F[Cache] =
    for {
      countries <- new EngineOpsCountry[F].createIndexIfNotExists
      settings <- new EngineOpsSettings[F].createIndexIfNotExists
    } yield Cache(settings, countries)

  private def logEngineStatus(status: String): F[Unit] = {
    val color = status.toLowerCase match {
      case "red" => "**** RED!! ****"
      case "yellow" => "** YELLOW **"
      case _ => status
    }

    L.info(s"Status of ES cluster(${clusterName(config)}) is ${color}")
  }
}
