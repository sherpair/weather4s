package io.sherpair.w4s.geo.engine

import cats.effect.Sync
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.w4s.config.{Suggestions => Parameters}
import io.sherpair.w4s.domain.{Countries, Country, Logger, Meta, Suggestions}
import io.sherpair.w4s.domain.Meta.id
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.geo.cache.Cache
import io.sherpair.w4s.geo.config.GeoConfig

class EngineOps[F[_]: Sync] (
    clusterName: String,
    engineOpsCountries: EngineOpsCountries[F],
    engineOpsLocality: EngineOpsLocality[F],
    engineOpsMeta: EngineOpsMeta[F]
  )(implicit E: Engine[F], L: Logger[F]) {

  def init: F[Cache] =
    for {
      (attempts, status) <- E.healthCheck
      _ <- logEngineStatus(attempts, status)
      cache <- E.execUnderGlobalLock[Cache](createIndexesIfNotExist)
    } yield cache

  def close: F[Unit] =
    L.info(s"Closing connection with ES cluster(${clusterName})") *> E.close

  def loadCountries: F[Countries] = engineOpsCountries.loadCountries

  def loadMeta: F[Option[Meta]] = engineOpsMeta.loadMeta

  def suggest(country: Country, localityTerm: String, parameters: Parameters): F[Suggestions] =
    engineOpsLocality.suggest(country, localityTerm, parameters)

  def suggestByAsciiOnly(country: Country, localityTerm: String, parameters: Parameters): F[Suggestions] =
    engineOpsLocality.suggestByAsciiOnly(country, localityTerm, parameters)

  def upsertCountry(country: Country): F[String] = engineOpsCountries.upsert(country)

  def upsertMeta(meta: Meta): F[String] = engineOpsMeta.upsert(meta)

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

  def apply[F[_]: Sync](clusterName: String)(implicit C: GeoConfig, E: Engine[F], L: Logger[F]): F[EngineOps[F]] =
    for {
      countryIndex <- E.engineIndex[Country](Country.indexName, _.code)
      engineOpsCountries <- EngineOpsCountries[F](countryIndex)

      localityIndex <- E.localityIndex
      engineOpsLocality <- EngineOpsLocality[F](localityIndex)

      metaIndex <- E.engineIndex[Meta](Meta.indexName, _ => id)
      engineOpsMeta <- EngineOpsMeta[F](metaIndex)
    }
    yield new EngineOps[F](clusterName, engineOpsCountries, engineOpsLocality, engineOpsMeta)
}
