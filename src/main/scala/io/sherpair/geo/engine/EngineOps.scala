package io.sherpair.geo.engine

import scala.io.Source.fromResource

import cats.effect.{Resource, Sync}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.algebra.Engine
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.config.Configuration._
import io.sherpair.geo.domain.{epochAsLong, Countries, Country, GeoError}

class EngineOps[F[_]](implicit config: Configuration, engine: Engine[F], L: Logger[F], S: Sync[F]) {
  def init: F[Unit] =
    for {
      status <- engine.init
      _ <- logEngineStatus(status)
      _ <- engine.execUnderGlobalLock[Countries](createIndexCountriesIfNotExists)
    } yield ()

  def close: F[Unit] =
    L.info(s"Closing connection with ES cluster(${clusterName(config)})") *> engine.close

  private def createIndexCountriesIfNotExists: F[Countries] =
    for {
      indexExists <- engine.indexExists(Country.indexName)
      countries <- if (indexExists) loadCountriesFromEngine else loadCountriesFromResource
    } yield countries

  private def decodeAndStoreCountries(json: String): F[Countries] =
    for {
      countries <- Country.decodeFromJson[F](json)
      _ <- engine.createIndex(Country.indexName, Country.mapping)
      _ <- logIndexStatus(Country.indexName, "was created")
      maybeAddAllFailure <- engine.addAll(Country.encodeForElastic(Country.indexName, countries))
      _ <- logCountOfStoredCountriesIfNotAFailure(countries.size, maybeAddAllFailure)
    } yield countries

  private def loadCountriesFromEngine: F[Countries] =
    for {
      _ <- logIndexStatus(Country.indexName, "already exists")
      response <- engine.queryAll(Country.indexName)
      countries <- S.delay(Country.decodeFromElastic(response))
      _ <- logCountOfCountries(countries)
    } yield countries

  private def loadCountriesFromResource: F[Countries] =
    Resource
      .fromAutoCloseable(S.delay(fromResource(Country.jsonFile)))
      .use(source => decodeAndStoreCountries(source.mkString))

  private def logCountOfCountries(countries: Countries): F[Unit] = {
    val size = countries.size
    val loadedFromUser = countries.count(_.updated != epochAsLong)
    L.info(s"Countries(${size}):  user-loaded(${loadedFromUser}),  not-loaded-yet(${size - loadedFromUser})")
  }

  private def logCountOfStoredCountriesIfNotAFailure(size: Int, maybeAddAllFailure: Option[String]): F[Unit] =
    if (maybeAddAllFailure.isDefined) S.raiseError(GeoError(maybeAddAllFailure.get))
    else L.info(s"Stored not-loaded-yet countries(${size})")

  private def logIndexStatus(indexName: String, status: String): F[Unit] =
    L.info(s"Index(${indexName}) ${status} in ES cluster(${clusterName(config)})")

  private def logEngineStatus(status: String): F[Unit] = {
    val color = status.toLowerCase match {
      case "red" => "**** RED!! ****"
      case "yellow" => "** YELLOW **"
      case _ => status
    }

    L.info(s"Status of ES cluster(${clusterName(config)}) is ${color}")
  }
}
