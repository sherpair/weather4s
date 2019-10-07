package io.sherpair.w4s.geo.engine

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.w4s.domain.{epochAsLong, toIsoDate, Logger, Meta}
import io.sherpair.w4s.domain.Meta.{id, indexName}
import io.sherpair.w4s.engine.{Engine, EngineIndex}

private[engine] class EngineOpsMeta[F[_]: Sync](implicit E: Engine[F], L: Logger[F]) {

  private[engine] val engineMeta: F[EngineIndex[F, Meta]] = E.engineIndex[Meta](indexName, _ => id)

  def count: F[Long] = engineMeta.flatMap(_.count)

  def createIndexIfNotExists: F[Meta] =
    E.indexExists(indexName).ifM(firstMetaLoad, initialiseMeta)

  def loadMeta: F[Option[Meta]] = engineMeta.flatMap(_.getById(id))

  def upsert(meta: Meta): F[String] = engineMeta.flatMap(_.upsert(meta))

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
      eM <- engineMeta
      _ <- eM.upsert(meta)
      _ <- E.refreshIndex(indexName)
    } yield meta

  private def logIndexStatus(status: String): F[Unit] =
    L.info(s"Index(${indexName}) ${status}")
}

object EngineOpsMeta {
  def apply[F[_]: Logger: Sync](implicit E: Engine[F]): EngineOpsMeta[F] = new EngineOpsMeta[F]
}
