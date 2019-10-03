package io.sherpair.w4s.engine.memory

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.w4s.domain.{BulkError, Country, Localities, Locality}
import io.sherpair.w4s.engine.LocalityIndex

class MemoryLocalityIndex[F[_]: Sync] extends LocalityIndex[F]{

  private val eI = Ref.of[F, Map[String, Map[String, Locality]]](Map.empty[String, Map[String, Locality]]).map(ref =>
    new LocalityIndex[F] {
      override def count(country: Country): F[Long] = ref.get.map(_.get(country.code).map(_.size.toLong).getOrElse(-1))
      override def getById(country: Country, id: String): F[Option[Locality]] = ref.get.map(_.get(country.code).flatMap(_.get(id)))

      override def delete(country: Country): F[Unit] = ref.update(_ - country.code)
      override def saveAll(country: Country, localities: Localities): F[List[BulkError]] = {
        ref.update(map =>
          map + (country.code -> localities.foldLeft(Map.empty[String, Locality]) { (map, l) => map + (l.geoId -> l) })
        ) *> Sync[F].delay(List.empty[BulkError])
      }

      private def get(country: Country): F[Option[Map[String, Locality]]] = ref.get.map(_.get(country.code))
    }
  )

  override def count(country: Country): F[Long] = eI.flatMap(_.count(country))
  override def getById(country: Country, id: String): F[Option[Locality]] = eI.flatMap(_.getById(country, id))
  override def delete(country: Country): F[Unit] = eI.flatMap(_.delete(country))
  override def saveAll(country: Country, localities: Localities): F[List[BulkError]] = eI.flatMap(_.saveAll(country, localities))
}

object MemoryLocalityIndex {
  def apply[F[_]: Sync]: LocalityIndex[F] = new MemoryLocalityIndex[F]
}
