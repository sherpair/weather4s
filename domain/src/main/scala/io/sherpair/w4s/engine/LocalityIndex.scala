package io.sherpair.w4s.engine

import io.sherpair.w4s.config.{Suggestions => Parameters}
import io.sherpair.w4s.domain.{BulkError, Country, Localities, Locality, Suggestions}

trait LocalityIndex[F[_]] {

  def count(country: Country): F[Long]

  // Test-only. Not used by the app.
  def getById(country: Country, id: String): F[Option[Locality]]

  def delete(country: Country): F[Unit]

  def saveAll(country: Country, localities: Localities): F[List[BulkError]]

  def suggest(country: Country, localityTerm: String, parameters: Parameters): F[Suggestions]

  def suggestByAsciiOnly(country: Country, localityTerm: String, parameters: Parameters): F[Suggestions]
}
