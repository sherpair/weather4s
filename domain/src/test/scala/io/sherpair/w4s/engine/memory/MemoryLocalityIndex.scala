package io.sherpair.w4s.engine.memory

import cats.effect.Concurrent
import cats.effect.concurrent.{Ref, Semaphore}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.w4s.config.{Suggestions => Parameters}
import io.sherpair.w4s.domain.{unit, BulkError, Country, Localities, Locality, Suggestions}
import io.sherpair.w4s.engine.LocalityIndex

object MemoryLocalityIndex {
  def apply[F[_]: Concurrent]: F[LocalityIndex[F]] =
    for {
      sem <- Semaphore[F](1)
      ref <- Ref.of[F, Map[String, Map[String, Locality]]](Map.empty[String, Map[String, Locality]])
    }
    yield new LocalityIndex[F] {

      override def count(country: Country): F[Long] =
        ref.get.map(_.get(country.code).map(_.size.toLong).getOrElse(-1))

      override def getById(country: Country, geoId: String): F[Option[Locality]] =
        ref.get.map(_.get(country.code).flatMap(_.get(geoId)))

      override def delete(country: Country): F[Unit] =
        sem.withPermit {
          for {
            map <- ref.get
            _ <- ref.set(map - country.code)
          }
          yield unit
        }

      override def saveAll(country: Country, localities: Localities): F[List[BulkError]] =
        sem.withPermit {
          for {
            map <- ref.get
            newMap <- Concurrent[F].delay(
              map + (country.code -> localities.foldLeft(Map.empty[String, Locality]) {
                (map, l) => map + (l.geoId -> l)
              })
            )
            _ <- ref.set(newMap)
          }
          yield List.empty[BulkError]
        }

      override def suggest(country: Country, localityTerm: String, parameters: Parameters): F[Suggestions] = ???

      override def suggestByAsciiOnly(
          country: Country, localityTerm: String, parameters: Parameters
      ): F[Suggestions] = ???
    }
}
