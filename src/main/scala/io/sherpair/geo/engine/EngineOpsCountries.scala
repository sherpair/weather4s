package io.sherpair.geo.engine

import scala.io.Source.fromResource

import cats.effect.{Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.domain._
import io.sherpair.geo.engine.EngineCountry.indexName

private[engine] class EngineOpsCountries[F[_]](engine: Engine[F])(implicit L: Logger[F], S: Sync[F]) {

  val bulkErrorMessage: String = "Got one or more errors while storing countries to the engine:\n"

  private val jsonFile: String = "countries.json"

  private val engineCountry: F[EngineCountry[F]] = engine.engineCountry

  def createIndexIfNotExists: F[Countries] =
    engine.indexExists(indexName).ifM(firstLoadOfCountriesFromEngine, firstLoadOfCountriesFromResource)

  def loadCountries: F[Countries] = engineCountry.flatMap(_.loadAll())

  def upsert(country: Country): F[String] = engineCountry.flatMap(_.upsert(country))

  private def decodeAndStoreCountries(json: String): F[Countries] =
    for {
      countries <- Country.decodeFromJson[F](json)
      _ <- engine.createIndex(indexName)
      _ <- logIndexStatus("was created")
      eC <- engineCountry
      listOfBulkErrors <- eC.saveAll(countries)
      _ <- logCountOfStoredCountriesIfNoErrors(countries.size, listOfBulkErrors)
    } yield countries

  private def firstLoadOfCountriesFromEngine: F[Countries] =
    for {
      _ <- logIndexStatus("already exists")
      countries <- loadCountries
      _ <- logCountOfCountriesLoadedFromEngine(countries)
    } yield countries

  private def firstLoadOfCountriesFromResource: F[Countries] =
    Resource
      .fromAutoCloseable(S.delay(fromResource(jsonFile)))
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
