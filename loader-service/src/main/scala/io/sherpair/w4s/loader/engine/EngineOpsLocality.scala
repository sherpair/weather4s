package io.sherpair.w4s.loader.engine

import cats.effect.Sync
import io.sherpair.w4s.domain.{BulkError, Country, Localities, Locality, Logger}
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.engine.LocalityIndex

private[engine] class EngineOpsLocality[F[_]: Sync](implicit E: Engine[F], L: Logger[F]) {

  private[engine] val localityIndex: LocalityIndex[F] = E.localityIndex

  def count(country: Country): F[Long] =
    localityIndex.count(country)

  def delete(country: Country): F[Unit] =
    localityIndex.delete(country)

  def find(country: Country, locality: Locality): F[Option[Locality]] =
    localityIndex.getById(country, locality.geoId)

  def saveAll(country: Country, localities: Localities): F[List[BulkError]] =
    localityIndex.saveAll(country, localities)
}

object EngineOpsLocality {
  def apply[F[_]: Logger: Sync](implicit E: Engine[F]): EngineOpsLocality[F] = new EngineOpsLocality[F]
}
