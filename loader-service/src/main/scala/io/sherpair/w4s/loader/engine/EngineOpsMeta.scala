package io.sherpair.w4s.loader.engine

import cats.effect.Sync
import io.sherpair.w4s.domain.Meta
import io.sherpair.w4s.domain.Meta.{id, indexName}
import io.sherpair.w4s.engine.{Engine, EngineIndex}

private[engine] class EngineOpsMeta[F[_]: Sync](metaIndex: EngineIndex[F, Meta])(implicit E: Engine[F]) {

  def find: F[Option[Meta]] = metaIndex.getById(id)

  def refresh: F[Boolean] = E.refreshIndex(indexName)

  def upsert(meta: Meta): F[String] = metaIndex.upsert(meta)
}

object EngineOpsMeta {

  def apply[F[_]: Sync](metaIndex: EngineIndex[F, Meta])(implicit E: Engine[F]): F[EngineOpsMeta[F]] =
    Sync[F].delay(new EngineOpsMeta[F](metaIndex))
}
