package io.sherpair.w4s.engine.memory

import scala.reflect.ClassTag

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.{Decoder, Encoder}
import io.sherpair.w4s.domain.{unit, BulkError}
import io.sherpair.w4s.engine.{Engine, EngineIndex}

trait Indexes[F[_]] {
  def createIndex(name: String): F[Unit]
  def indexExists(name: String): F[Boolean]
}

class MemoryEngine[F[_]: Sync] extends Engine[F] {

  private val indexes: F[Indexes[F]] = Ref.of[F, Set[String]](Set.empty[String]).map(ref =>
    new Indexes[F] {
      def createIndex(name: String): F[Unit] = ref.get.map(names => names + name).flatTap(ref.set).void
      def indexExists(name: String): F[Boolean] = ref.get.map(_.contains(name))
    }
  )

  override def close: F[Unit] = Sync[F].unit
  override def createIndex(name: String, jsonMapping: Option[String]): F[Unit] = indexes.map(_.createIndex(name)).void

  override def engineIndex[T: ClassTag: Decoder: Encoder](indexName: String, f: T => String): EngineIndex[F, T] =
    MemoryEngineIndex[F, T](indexName, f)

  override def execUnderGlobalLock[T](f: => F[T]): F[T] = f
  override def healthCheck: F[(Int, String)] = Sync[F].delay((1, "green"))
  override def indexExists(name: String): F[Boolean] = indexes.flatMap(_.indexExists(name))
}

object MemoryEngine {
  def apply[F[_]: Sync]: Engine[F] = new MemoryEngine[F]

  def applyWithFailingSaveAll[F[_]: Sync](bulkErrors: => List[BulkError]): Engine[F] =
    new MemoryEngine[F] {
      override def engineIndex[T: ClassTag: Decoder: Encoder](indexName: String, f: T => String): EngineIndex[F, T] =
        MemoryEngineIndexWithFailingSaveAll[F, T](indexName, f, bulkErrors)
    }
}
