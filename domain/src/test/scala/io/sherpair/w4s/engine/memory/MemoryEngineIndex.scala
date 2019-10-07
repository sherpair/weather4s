package io.sherpair.w4s.engine.memory

import cats.effect.Concurrent
import cats.effect.concurrent.{Ref, Semaphore}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.w4s.domain.BulkError
import io.sherpair.w4s.engine.EngineIndex

object MemoryEngineIndex {
  def apply[F[_]: Concurrent, T](indexName: String, f: T => String): F[EngineIndex[F, T]] =
    for {
      sem <- Semaphore[F](1)
      ref <- Ref.of[F, Map[String, T]](Map.empty[String, T])
    }
    yield new EngineIndex[F, T] {
      override def count: F[Long] = ref.get.map(_.size.toLong)
      override def getById(id: String): F[Option[T]] = ref.get.map(_.get(id))
      override def loadAll(sortBy: Option[Seq[String]], windowSize: Int): F[List[T]] = ref.get.map(_.values.toList)

      override def saveAll(documents: List[T]): F[List[BulkError]] =
        sem.withPermit {
          for {
            newMap <- Concurrent[F].delay(
              documents.foldLeft(Map.empty[String, T]) {(map, document) => map + (f(document) -> document)}
            )
            _ <- ref.set(newMap)
          }
          yield List.empty[BulkError]
        }

      override def upsert(document: T): F[String] =
        sem.withPermit {
          for {
            map <- ref.get
            _ <- ref.set(map + (f(document) -> document))
          }
          yield "OK"
        }
    }
}

//object MemoryEngineIndexWithFailingSaveAll {
//  def apply[F[_]: Sync, T](indexName: String, f: T => String, bulkErrors: => List[BulkError]): EngineIndex[F, T] =
//    new MemoryEngineIndex[F, T](indexName, f) {
//      override def saveAll(documents: List[T]): F[List[BulkError]] = Sync[F].delay(bulkErrors)
//    }
//}
