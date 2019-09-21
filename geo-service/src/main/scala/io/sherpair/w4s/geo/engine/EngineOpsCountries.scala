package io.sherpair.w4s.geo.engine

import scala.io.Source.fromResource

import cats.effect.{Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.w4s.domain.{epochAsLong, BulkError, Countries, Country, Meta}
import io.sherpair.w4s.domain.Country.indexName
import io.sherpair.w4s.engine.{Engine, EngineIndex}
import io.sherpair.w4s.engine.EngineIndex.bulkErrorMessage

private[engine] class EngineOpsCountries[F[_]: Sync](implicit E: Engine[F], L: Logger[F]) {

  private[engine] val engineCountry: EngineIndex[F, Country] = E.engineIndex[Country](indexName, _.code)

  private val jsonFile: String = "countries.json"

  def count: F[Long] = engineCountry.count

  def createIndexIfNotExists: F[Countries] =
    E.indexExists(indexName).ifM(firstLoadOfCountriesFromEngine, firstLoadOfCountriesFromResource)

  def loadCountries: F[Countries] = engineCountry.loadAll()

  def upsert(country: Country): F[String] = engineCountry.upsert(country)

  private def decodeAndStoreCountries(json: String): F[Countries] =
    for {
      countries <- Country.decodeFromJson[F](json)
      _ <- E.createIndex(indexName)
      _ <- logIndexStatus("was created")
      listOfBulkErrors <- engineCountry.saveAll(countries)
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
      .fromAutoCloseable(Sync[F].delay(fromResource(jsonFile)))
      .use(source => decodeAndStoreCountries(source.mkString))

  private def logCountOfCountriesLoadedFromEngine(countries: Countries): F[Unit] = {
    val size = countries.size
    require(size >= Country.numberOfCountries, Country.requirement)  // Fatal Error!!
    val loadedFromUser = countries.count(_.updated != epochAsLong)
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
  def apply[F[_]: Logger: Sync](implicit E: Engine[F]): EngineOpsCountries[F] = new EngineOpsCountries[F]()
}
