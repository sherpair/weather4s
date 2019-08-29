package io.sherpair.geo.engine

import scala.io.Source.fromResource

import cats.effect.{Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.domain._

private[engine] class EngineOpsCountries[F[_]](implicit engine: Engine[F], L: Logger[F], S: Sync[F]) {

  def createIndexIfNotExists: F[Countries] =
    engine.indexExists(Country.indexName).ifM(firstLoadOfCountriesFromEngine, firstLoadOfCountriesFromResource)

  def loadCountries: F[Countries] =
    for {
      response <- engine.queryAll(Country.indexName)
      countries <- S.delay(Country.decodeFromElastic(response))
    } yield countries

  private def decodeAndStoreCountries(json: String): F[Countries] =
    for {
      countries <- Country.decodeFromJson[F](json)
      _ <- engine.createIndex(Country.indexName, Country.mapping)
      _ <- logIndexStatus("was created")
      maybeFailure <- engine.addAll(Country.encodeForElastic(Country.indexName, countries))
      _ <- logCountOfStoredCountriesIfNotAFailure(countries.size, maybeFailure)
    } yield countries

  private def firstLoadOfCountriesFromEngine: F[Countries] =
    for {
      _ <- logIndexStatus("already exists")
      countries <- loadCountries
      _ <- Country.logCountOfCountries[F](countries)
    } yield countries

  private def firstLoadOfCountriesFromResource: F[Countries] =
    Resource
      .fromAutoCloseable(S.delay(fromResource(Country.jsonFile)))
      .use(source => decodeAndStoreCountries(source.mkString))

  private def logCountOfStoredCountriesIfNotAFailure(size: Int, maybeFailure: Option[String]): F[Unit] =
    if (maybeFailure.isDefined) S.raiseError(GeoError(maybeFailure.get))
    else L.info(s"Countries not-uploaded-yet(${size})")

  private def logIndexStatus(status: String): F[Unit] =
    L.info(s"Index(${Country.indexName}) ${status}")
}
