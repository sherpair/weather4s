package io.sherpair.w4s.engine.memory

import cats.effect.Concurrent
import cats.effect.concurrent.Ref
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.functor._
import io.sherpair.w4s.domain.BulkErrors
import io.sherpair.w4s.engine.EngineIndex

object MemoryEngineIndex {

  def apply[F[_]: Concurrent, T](
      indexName: String, f: T => String)(implicit resultForSaveAll: BulkErrors
  ): F[EngineIndex[F, T]] =

    Ref.of[F, Map[String, T]](Map.empty[String, T]).map { ref: Ref[F, Map[String, T]] =>
      new EngineIndex[F, T] {
        override def count: F[Long] = ref.get.map(_.size.toLong)

        override def getById(id: String): F[Option[T]] = ref.get.map(_.get(id))

        override def loadAll(sortBy: Option[Seq[String]], windowSize: Int): F[List[T]] =
          ref.get.map(_.values.toList)

        override def saveAll(documents: List[T]): F[BulkErrors] =
          ref.update {
            _ ++ documents.foldLeft(Map.empty[String, T]) { (map, document) =>
              map + (f(document) -> document)
            }
          } *> resultForSaveAll.pure[F]

        override def upsert(document: T): F[String] =
          ref.update(update(_, document)) *> "OK".pure[F]

        private def update(map: Map[String, T], document: T): Map[String, T] =
          map.updated(f(document), document)
      }
    }
}
