package io.sherpair.geo.engine

import io.sherpair.geo.domain.{BulkError, Countries, Country}

trait EngineCountry[F[_]] {

  // scalastyle:off magic.number
  private val DefaultWindowSize: Int = 1000
  // scalastyle:on magic.number

  // Test-only. Not used by the app.
  def getById(id: String): F[Option[Country]]

  /*
   * 0 < windowSize param <= MaxWindowSize
   */
  def loadAll(sortBy: Option[Seq[String]] = None, windowSize: Int = DefaultWindowSize): F[Countries]

  def saveAll(countries: Countries): F[List[BulkError]]

  // Test-only. Not used by the app.
  def upsert(country: Country): F[String]
}

object EngineCountry {

  val indexName = "countries"
}
