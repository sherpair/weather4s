package io.sherpair.w4s.engine

import io.sherpair.w4s.domain.{BulkError, Country, Localities, Locality}

trait LocalityIndex[F[_]] {

  def count(country: Country): F[Long]

  // Test-only. Not used by the app.
  def getById(country: Country, id: String): F[Option[Locality]]

  def delete(country: Country): F[Unit]

  def saveAll(country: Country, localities: Localities): F[List[BulkError]]
}
