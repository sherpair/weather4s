package io.sherpair.w4s.loader.engine

import cats.effect.Sync
import io.sherpair.w4s.domain.Country
import io.sherpair.w4s.domain.Country.indexName
import io.sherpair.w4s.engine.{Engine, EngineIndex}

private[engine] class EngineOpsCountries[F[_]: Sync](countryIndex: EngineIndex[F, Country])(implicit E: Engine[F]) {

  def find(country: Country): F[Option[Country]] = countryIndex.getById(country.code)

  def refresh: F[Boolean] = E.refreshIndex(indexName)

  def upsert(country: Country): F[String] = countryIndex.upsert(country)
}

object EngineOpsCountries {

  def apply[F[_]: Sync](countryIndex: EngineIndex[F, Country])(implicit E: Engine[F]): F[EngineOpsCountries[F]] =
    Sync[F].delay(new EngineOpsCountries[F](countryIndex))
}
