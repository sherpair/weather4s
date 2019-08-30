package io.sherpair.geo.engine

import scala.io.Source.fromResource

import cats.effect.{Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.domain._

private[engine] class EngineOpsCountries[F[_]](implicit engine: Engine[F], L: Logger[F], S: Sync[F]) {

  private val jsonFile: String = "countries.json"

  private val engineCountry = engine.engineCountry

  def createIndexIfNotExists: F[Countries] =
    engine.indexExists(engineCountry.indexName).ifM(firstLoadOfCountriesFromEngine, firstLoadOfCountriesFromResource)

  def loadCountries: F[Countries] = engineCountry.loadAll()

  private def decodeAndStoreCountries(json: String): F[Countries] =
    for {
      countries <- Country.decodeFromJson[F](json)
      _ <- engine.createIndex(engineCountry.indexName)
      _ <- logIndexStatus("was created")
      listOfBulkErrors <- engineCountry.addInBulk(countries)
      _ <- logCountOfStoredCountriesIfNoErrors(countries.size, listOfBulkErrors)
    } yield countries

  private def firstLoadOfCountriesFromEngine: F[Countries] =
    for {
      _ <- logIndexStatus("already exists")
      countries <- loadCountries
      _ <- Country.logCountOfCountries[F](countries)
    } yield countries

  private def firstLoadOfCountriesFromResource: F[Countries] =
    Resource
      .fromAutoCloseable(S.delay(fromResource(jsonFile)))
      .use(source => decodeAndStoreCountries(source.mkString))

  private def logCountOfStoredCountriesIfNoErrors(size: Int, listOfBulkErrors: List[BulkError]): F[Unit] =
    if (listOfBulkErrors.nonEmpty) {
      S.raiseError(GeoError(listOfBulkErrors.mkString("One or more fatal errors while storing countries to the engine:\n", ",\n", "\n")))
    }
    else L.info(s"Countries not-uploaded-yet(${size})")

  private def logIndexStatus(status: String): F[Unit] =
    L.info(s"Index(${engineCountry.indexName}) ${status}")
}
