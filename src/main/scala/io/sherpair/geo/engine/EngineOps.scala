package io.sherpair.geo.engine

import cats.effect.{Resource, Sync}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.cache.Cache
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.config.Configuration._
import io.sherpair.geo.domain.{Countries, Meta}

class EngineOps[F[_]] private (implicit config: Configuration, engine: Engine[F], L: Logger[F], S: Sync[F]) {

  val engineOpsCountries = new EngineOpsCountries[F]
  val engineOpsMeta = new EngineOpsMeta[F]

  def init: F[Cache] =
    for {
      status <- engine.healthCheck
      _ <- logEngineStatus(status)
      cache <- engine.execUnderGlobalLock[Cache](createIndexesIfNotExist)
    } yield cache

  def close: F[Unit] =
    L.info(s"Closing connection with ES cluster(${clusterName(config)})") *> engine.close

  def loadMeta: F[Meta] = engineOpsMeta.loadMeta

  def loadCountries: F[Countries] = engineOpsCountries.loadCountries

  private def createIndexesIfNotExist: F[Cache] =
    for {
      countries <- engineOpsCountries.createIndexIfNotExists
      meta <- engineOpsMeta.createIndexIfNotExists
    } yield Cache(meta.lastEngineUpdate, countries)

  private def logEngineStatus(status: String): F[Unit] = {
    val color = status.toLowerCase match {
      case "red" => "**** RED!! ****"
      case "yellow" => "** YELLOW **"
      case _ => status
    }

    L.info(s"Status of ES cluster(${clusterName(config)}) is ${color}")
  }
}

object EngineOps {
  def apply[F[_]: Engine: Logger: Sync](implicit C: Configuration): Resource[F, EngineOps[F]] =
    Resource.liftF(Sync[F].delay(new EngineOps[F]))
}
