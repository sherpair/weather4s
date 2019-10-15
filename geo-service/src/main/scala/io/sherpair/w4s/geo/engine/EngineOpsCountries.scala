package io.sherpair.w4s.geo.engine

import scala.io.Source.fromResource

import cats.effect.{Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.w4s.domain.{epochAsLong, BulkError, Countries, Country, Logger}
import io.sherpair.w4s.domain.Country.indexName
import io.sherpair.w4s.engine.{Engine, EngineIndex}
import io.sherpair.w4s.engine.EngineIndex.bulkErrorMessage
import io.sherpair.w4s.geo.config.GeoConfig

private[engine] class EngineOpsCountries[F[_]: Sync](
    countryIndex: EngineIndex[F, Country])(implicit C: GeoConfig, E: Engine[F], L: Logger[F]
) {
  def count: F[Long] = countryIndex.count

  def createIndexIfNotExists: F[Countries] =
    E.indexExists(indexName).ifM(firstLoadOfCountriesFromEngine, firstLoadOfCountriesFromResource)

  def loadCountries: F[Countries] = countryIndex.loadAll()

  // Must only be used for testing
  def upsert(country: Country): F[String] = countryIndex.upsert(country)

  private def decodeAndStoreCountries(json: String): F[Countries] =
    for {
      countries <- Country.decodeFromJson[F](json)
      _ <- E.createIndex(indexName)
      _ <- logIndexStatus("was created")
      listOfBulkErrors <- countryIndex.saveAll(countries)
      _ <- E.refreshIndex(indexName)
      _ <- logCountOfStoredCountriesIfNoErrors(countries.size, listOfBulkErrors)
    } yield countries

  private[engine] def firstLoadOfCountriesFromEngine: F[Countries] =
    for {
      _ <- logIndexStatus("already exists")
      countries <- loadCountries
      _ <- logCountOfCountriesLoadedFromEngine(countries)
    } yield countries

  private[engine] def firstLoadOfCountriesFromResource: F[Countries] =
    Resource
      .fromAutoCloseable(Sync[F].delay(fromResource(C.countries)))
      .use(source => decodeAndStoreCountries(source.mkString))

  private def logCountOfCountriesLoadedFromEngine(countries: Countries): F[Unit] = {
    val size = countries.size
    require(size >= Country.numberOfCountries, Country.requirement)  // Fatal Error!!
    val loadedFromUser = countries.count(_.updated > epochAsLong)
    L.info(s"Countries(${size}):  uploaded(${loadedFromUser}),  not-uploaded-yet(${size - loadedFromUser})")
  }

  private def logCountOfStoredCountriesIfNoErrors(size: Int, listOfBulkErrors: List[BulkError]): F[Unit] = {
    require(listOfBulkErrors.isEmpty, listOfBulkErrors.mkString(bulkErrorMessage, ",\n", "\n"))  // Fatal Error!!
    L.info(s"Countries not-uploaded-yet(${size})")
  }

  private def logIndexStatus(status: String): F[Unit] =
    L.info(s"Index(${indexName}) ${status}")
}

object EngineOpsCountries {

  def apply[F[_]: Sync](
      countryIndex: EngineIndex[F, Country])(implicit C: GeoConfig, E: Engine[F], L: Logger[F]
  ): F[EngineOpsCountries[F]] = Sync[F].delay(new EngineOpsCountries[F](countryIndex))
}
