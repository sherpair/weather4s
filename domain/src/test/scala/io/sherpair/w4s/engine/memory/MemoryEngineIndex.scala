package io.sherpair.w4s.engine.memory

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.w4s.domain.BulkError
import io.sherpair.w4s.engine.EngineIndex

class MemoryEngineIndex[F[_]: Sync, T](indexName: String, f: T => String) extends EngineIndex[F, T] {

  private val eI = Ref.of[F, Map[String, T]](Map.empty[String, T]).map(ref =>
    new EngineIndex[F, T] {
      override def count: F[Long] = ref.get.map(_.size.toLong)
      override def getById(id: String): F[Option[T]] = ref.get.map(_.get(id))
      override def loadAll(sortBy: Option[Seq[String]], windowSize: Int): F[List[T]] = ref.get.map(_.values.toList)
      override def saveAll(documents: List[T]): F[List[BulkError]] = {
        ref.set(
          documents.foldLeft(Map.empty[String, T]) { (map, document) => map + (f(document) -> document) }
        ) *> Sync[F].delay(List.empty[BulkError])
      }
      override def upsert(document: T): F[String] = ref.update(_ + (f(document) -> document)) *> "OK".pure[F]
    }
  )

  override def count: F[Long] = eI.flatMap(_.count)
  override def getById(id: String): F[Option[T]] = eI.flatMap(_.getById(id))
  override def loadAll(sortBy: Option[Seq[String]], windowSize: Int): F[List[T]] = eI.flatMap(_.loadAll())
  override def saveAll(documents: List[T]): F[List[BulkError]] = eI.flatMap(_.saveAll(documents))
  override def upsert(document: T): F[String] = eI.flatMap(_.upsert(document))
}

object MemoryEngineIndex {
  def apply[F[_]: Sync, T](indexName: String, f: T => String): EngineIndex[F, T] =
    new MemoryEngineIndex[F, T](indexName, f)
}

object MemoryEngineIndexWithFailingSaveAll {
  def apply[F[_]: Sync, T](indexName: String, f: T => String, bulkErrors: => List[BulkError]): EngineIndex[F, T] =
    new MemoryEngineIndex[F, T](indexName, f) {
      override def saveAll(documents: List[T]): F[List[BulkError]] = Sync[F].delay(bulkErrors)
    }
}
