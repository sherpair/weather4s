package io.sherpair.geo.engine

import io.sherpair.geo.domain.{BulkError, Countries, Country}

trait EngineCountry[F[_]] {

  val indexName: String = "countries"

  // scalastyle:off magic.number
  val DefaultWindowSize: Int = 1000
  val MaxWindowSize: Int = 10000
  // scalastyle:on magic.number

  def addInBulk(countries: Countries): F[List[BulkError]]

  def getById(id: String): F[Country]

  /*
   * 0 < windowSize param <= MaxWindowSize
   */
  def loadAll(sortBy: Option[Seq[String]] = None, windowSize: Int = DefaultWindowSize): F[Countries]

  def upsert(country: Country): F[String]
}
