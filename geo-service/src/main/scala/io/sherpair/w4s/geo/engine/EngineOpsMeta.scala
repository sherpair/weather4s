package io.sherpair.w4s.geo.engine

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.w4s.domain.{epochAsLong, toIsoDate, Logger, Meta}
import io.sherpair.w4s.domain.Meta.{id, indexName}
import io.sherpair.w4s.engine.{Engine, EngineIndex}

private[engine] class EngineOpsMeta[F[_]: Sync](metaIndex: EngineIndex[F, Meta])(implicit E: Engine[F], L: Logger[F]) {

  def count: F[Long] = metaIndex.count

  def createIndexIfNotExists: F[Meta] =
    E.indexExists(indexName).ifM(firstMetaLoad, initialiseMeta)

  def loadMeta: F[Option[Meta]] = metaIndex.getById(id)

  def upsert(meta: Meta): F[String] = metaIndex.upsert(meta)

  private def extractMetaAndLogLastEngineUpdate(maybeMeta: Option[Meta]): F[Meta] = {
    require(maybeMeta.isDefined, Meta.requirement)  // Fatal Error!!
    Sync[F].delay(maybeMeta.get)
  }

  private[engine] def firstMetaLoad: F[Meta] =
    for {
      _ <- logIndexStatus("already exists")
      maybeMeta <- loadMeta
      meta <- extractMetaAndLogLastEngineUpdate(maybeMeta)
      _ <- L.info(s"Last Engine update at(${toIsoDate(meta.lastEngineUpdate)})")
    } yield meta

  private[engine] def initialiseMeta: F[Meta] =
    for {
      _ <- E.createIndex(indexName)
      _ <- logIndexStatus("was created")
      meta = Meta(epochAsLong)
      _ <- metaIndex.upsert(meta)
      _ <- E.refreshIndex(indexName)
    } yield meta

  private def logIndexStatus(status: String): F[Unit] =
    L.info(s"Index(${indexName}) ${status}")
}

object EngineOpsMeta {

  def apply[F[_]: Sync](metaIndex: EngineIndex[F, Meta])(implicit E: Engine[F], L: Logger[F]): F[EngineOpsMeta[F]] =
    Sync[F].delay(new EngineOpsMeta[F](metaIndex))
}
