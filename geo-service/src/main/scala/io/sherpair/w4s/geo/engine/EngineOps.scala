package io.sherpair.w4s.geo.engine

import cats.effect.Sync
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.w4s.domain.{Countries, Meta}
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.geo.cache.Cache

class EngineOps[F[_]: Sync] (clusterName: String)(implicit E: Engine[F], L: Logger[F]) {

  val engineOpsCountries: EngineOpsCountries[F] = EngineOpsCountries[F]
  val engineOpsMeta: EngineOpsMeta[F] = EngineOpsMeta[F]

  def init: F[Cache] =
    for {
      (attempts, status) <- E.healthCheck
      _ <- logEngineStatus(attempts, status)
      cache <- E.execUnderGlobalLock[Cache](createIndexesIfNotExist)
    } yield cache

  def close: F[Unit] =
    L.info(s"Closing connection with ES cluster(${clusterName})") *> E.close

  def loadMeta: F[Option[Meta]] = engineOpsMeta.loadMeta

  def loadCountries: F[Countries] = engineOpsCountries.loadCountries

  private def createIndexesIfNotExist: F[Cache] =
    for {
      countries <- engineOpsCountries.createIndexIfNotExists
      meta <- engineOpsMeta.createIndexIfNotExists
    } yield Cache(meta.lastEngineUpdate, countries)

  private def logEngineStatus(attempts: Int, status: String): F[Unit] = {
    val color = status.toLowerCase match {
      case "red" => "**** RED!! ****"
      case "yellow" => "** YELLOW **"
      case _ => status
    }

    val healthCheck = s"Health check successful after ${attempts} ${if (attempts == 1) "attempt" else "attempts"}"
    L.info(s"\n${healthCheck}\nStatus of ES cluster(${clusterName}) is ${color}")
  }
}

object EngineOps {
  def apply[F[_]: Engine: Logger: Sync](clusterName: String): EngineOps[F] = new EngineOps[F](clusterName)
}
