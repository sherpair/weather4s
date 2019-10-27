package io.sherpair.w4s.engine.memory

import scala.reflect.ClassTag

import cats.effect.Concurrent
import cats.effect.concurrent.Ref
import cats.syntax.applicative._
import cats.syntax.functor._
import io.circe.{Decoder, Encoder}
import io.sherpair.w4s.domain.BulkErrors
import io.sherpair.w4s.engine.{Engine, EngineIndex, LocalityIndex}

object MemoryEngine {

  def apply[F[_]: Concurrent](
      dataSuggesterMap: Map[String, DataSuggesters])(implicit resultForSaveAll: BulkErrors
  ): F[Engine[F]] =
    Ref.of[F, Set[String]](Set.empty[String]).map { ref: Ref[F, Set[String]] =>
      new Engine[F] {
        override def close: F[Unit] = Concurrent[F].unit
        override def createIndex(name: String, jsonMapping: Option[String]): F[Unit] = ref.update(_ + name)

        override def engineIndex[T: ClassTag: Decoder: Encoder](
          indexName: String, f: T => String
        ): F[EngineIndex[F, T]] = MemoryEngineIndex[F, T](indexName, f)

        override def execUnderGlobalLock[T](f: => F[T]): F[T] = f
        override def healthCheck: F[(Int, String)] = Concurrent[F].delay((1, "green"))
        override def indexExists(name: String): F[Boolean] = ref.get.map(_.contains(name))

        override def localityIndex: F[LocalityIndex[F]] = MemoryLocalityIndex[F](dataSuggesterMap)

        override def refreshIndex(name: String): F[Boolean] = true.pure[F]
      }
    }
}
