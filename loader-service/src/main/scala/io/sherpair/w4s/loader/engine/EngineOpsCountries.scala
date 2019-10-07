package io.sherpair.w4s.loader.engine

import cats.effect.Sync
import cats.syntax.flatMap._
import io.sherpair.w4s.domain.{Country, Logger}
import io.sherpair.w4s.domain.Country.indexName
import io.sherpair.w4s.engine.{Engine, EngineIndex}

private[engine] class EngineOpsCountries[F[_]: Sync](implicit E: Engine[F], L: Logger[F]) {

  private[engine] val engineCountry: F[EngineIndex[F, Country]] = E.engineIndex[Country](indexName, _.code)

  def find(country: Country): F[Option[Country]] = engineCountry.flatMap(_.getById(country.code))

  def refresh: F[Boolean] = E.refreshIndex(indexName)

  def upsert(country: Country): F[String] = engineCountry.flatMap(_.upsert(country))
}

object EngineOpsCountries {
  def apply[F[_]: Logger: Sync](implicit E: Engine[F]): EngineOpsCountries[F] = new EngineOpsCountries[F]()
}
