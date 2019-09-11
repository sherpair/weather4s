package io.sherpair.geo.engine.memory

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.geo.domain.{unit, GeoError}
import io.sherpair.geo.engine.{Engine, EngineCountry, EngineMeta}

class MemoryEngine[F[_]: Sync](
    indexes: Ref[F, Set[String]],
    memoryEngineCountry: MemoryEngineCountry[F],
    memoryEngineMeta: MemoryEngineMeta[F]
) extends Engine[F] {

  override def close: F[Unit] = unit.pure[F]

  override def count(indexName: String): F[Long] =
    indexName match {
      case EngineCountry.indexName => memoryEngineCountry.refCount
      case EngineMeta.indexName => memoryEngineMeta.refCount
      case _ => Sync[F].raiseError(GeoError(s"Bug in MemoryEngine: index(${indexName}) is unknown"))
    }

  override def createIndex(name: String, jsonMapping: Option[String]): F[Unit] =
    indexes.get
      .map(names => names + name)
      .flatTap(indexes.set).void

  override def engineCountry: F[EngineCountry[F]] = Sync[F].delay(memoryEngineCountry)

  override def engineMeta: F[EngineMeta[F]] = Sync[F].delay(memoryEngineMeta)

  override def execUnderGlobalLock[T](f: => F[T]): F[T] = f

  override def healthCheck: F[String] = "green".pure[F]

  override def indexExists(name: String): F[Boolean] = indexes.get.map(_.contains(name))
}

object MemoryEngine {
  def apply[F[_]: Sync]: F[MemoryEngine[F]] = _apply(MemoryEngineCountry[F])

  def applyWithFailingSaveAll[F[_]: Sync]: F[MemoryEngine[F]] = _apply(MemoryEngineCountryWithFailingSaveAll[F])

  private def _apply[F[_]: Sync](memoryEngineCountry: F[MemoryEngineCountry[F]]): F[MemoryEngine[F]] =
    for {
      indexes <- Ref.of[F, Set[String]](Set.empty)
      memoryEngineCountry <- memoryEngineCountry
      memoryEngineMeta <- MemoryEngineMeta[F]
    }
    yield new MemoryEngine[F](indexes, memoryEngineCountry, memoryEngineMeta)
}
