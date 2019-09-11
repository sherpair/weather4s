package io.sherpair.geo.engine

import cats.effect.Sync
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.cache.Cache
import io.sherpair.geo.domain.{Countries, Meta}

class EngineOps[F[_]: Sync] private (clusterName: String)(implicit engine: Engine[F], L: Logger[F]) {

  val engineOpsCountries = new EngineOpsCountries[F](engine)
  val engineOpsMeta = new EngineOpsMeta[F](engine)

  def init: F[Cache] =
    for {
      status <- engine.healthCheck
      _ <- logEngineStatus(status)
      cache <- engine.execUnderGlobalLock[Cache](createIndexesIfNotExist)
    } yield cache

  def close: F[Unit] =
    L.info(s"Closing connection with ES cluster(${clusterName})") *> engine.close

  def loadMeta: F[Option[Meta]] = engineOpsMeta.loadMeta

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

    L.info(s"Status of ES cluster(${clusterName}) is ${color}")
  }
}

object EngineOps {
  def apply[F[_]: Engine: Logger: Sync](clusterName: String): F[EngineOps[F]] =
    Sync[F].delay(new EngineOps[F](clusterName))
}
