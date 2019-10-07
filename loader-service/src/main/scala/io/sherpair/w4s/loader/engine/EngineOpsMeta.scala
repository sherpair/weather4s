package io.sherpair.w4s.loader.engine

import cats.effect.Sync
import cats.syntax.flatMap._
import io.sherpair.w4s.domain.{Logger, Meta}
import io.sherpair.w4s.domain.Meta.{id, indexName}
import io.sherpair.w4s.engine.{Engine, EngineIndex}

private[engine] class EngineOpsMeta[F[_]: Sync](implicit E: Engine[F], L: Logger[F]) {

  private[engine] val engineMeta: F[EngineIndex[F, Meta]] = E.engineIndex[Meta](indexName, _ => id)

  def find: F[Option[Meta]] = engineMeta.flatMap(_.getById(id))

  def refresh: F[Boolean] = E.refreshIndex(indexName)

  def upsert(meta: Meta): F[String] = engineMeta.flatMap(_.upsert(meta))
}

object EngineOpsMeta {
  def apply[F[_]: Logger: Sync](implicit E: Engine[F]): EngineOpsMeta[F] = new EngineOpsMeta[F]
}
