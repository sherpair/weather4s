package io.sherpair.geo.engine

import scala.io.Source.fromResource

import cats.effect.{Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.domain._

private[engine] class EngineOpsCountry[F[_]](implicit engine: Engine[F], L: Logger[F], S: Sync[F]) {

  def createIndexIfNotExists: F[Countries] =
    for {
      indexExists <- engine.indexExists(Country.indexName)
      countries <- if (indexExists) loadCountriesFromEngine else loadCountriesFromResource
    } yield countries

  private def decodeAndStoreCountries(json: String): F[Countries] =
    for {
      countries <- Country.decodeFromJson[F](json)
      _ <- engine.createIndex(Country.indexName, Country.mapping)
      _ <- logIndexStatus("was created")
      maybeFailure <- engine.addAll(Country.encodeForElastic(Country.indexName, countries))
      _ <- logCountOfStoredCountriesIfNotAFailure(countries.size, maybeFailure)
    } yield countries

  private def loadCountriesFromEngine: F[Countries] =
    for {
      _ <- logIndexStatus("already exists")
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
    L.info(s"Countries(${size}):  uploaded(${loadedFromUser}),  not-uploaded-yet(${size - loadedFromUser})")
  }

  private def logCountOfStoredCountriesIfNotAFailure(size: Int, maybeFailure: Option[String]): F[Unit] =
    if (maybeFailure.isDefined) S.raiseError(GeoError(maybeFailure.get))
    else L.info(s"Countries not-uploaded-yet(${size})")

  private def logIndexStatus(status: String): F[Unit] =
    L.info(s"Index(${Country.indexName}) ${status}")
}
