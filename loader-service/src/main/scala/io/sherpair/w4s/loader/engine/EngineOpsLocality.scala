package io.sherpair.w4s.loader.engine

import cats.effect.Sync
import io.sherpair.w4s.domain.{BulkErrors, Country, Locality}
import io.sherpair.w4s.engine.LocalityIndex
import io.sherpair.w4s.types.Localities

private[engine] class EngineOpsLocality[F[_]: Sync](localityIndex: LocalityIndex[F]) {

  def count(country: Country): F[Long] =
    localityIndex.count(country)

  def delete(country: Country): F[Unit] =
    localityIndex.delete(country)

  def find(country: Country, locality: Locality): F[Option[Locality]] =
    localityIndex.getById(country, locality.geoId)

  def saveAll(country: Country, localities: Localities): F[BulkErrors] =
    localityIndex.saveAll(country, localities)
}

object EngineOpsLocality {

  def apply[F[_]: Sync](localityIndex: LocalityIndex[F]): F[EngineOpsLocality[F]] =
    Sync[F].delay(new EngineOpsLocality[F](localityIndex))
}
