package io.sherpair.geo.engine

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.domain._
import io.sherpair.geo.engine.EngineMeta.indexName

private[engine] class EngineOpsMeta[F[_]: Sync](engine: Engine[F])(implicit L: Logger[F]) {

  private val engineMeta: F[EngineMeta[F]] = engine.engineMeta

  def createIndexIfNotExists: F[Meta] =
    engine.indexExists(indexName).ifM(firstMetaLoad, initialiseMeta)

  def loadMeta: F[Option[Meta]] = engineMeta.flatMap(_.getById)

  def upsert(meta: Meta): F[String] = engineMeta.flatMap(_.upsert(meta))

  private def extractMetaAndLogLastEngineUpdate(maybeMeta: Option[Meta]): F[Meta] = {
    require(maybeMeta.isDefined, Meta.requirement)  // Fatal Error!!
    Sync[F].delay(maybeMeta.get)
  }

  private def firstMetaLoad: F[Meta] =
    for {
      _ <- logIndexStatus("already exists")
      maybeMeta <- loadMeta
      meta <- extractMetaAndLogLastEngineUpdate(maybeMeta)
      _ <- L.info(s"Last Engine update at(${toIsoDate(meta.lastEngineUpdate)})")
    } yield meta

  private def initialiseMeta: F[Meta] =
    for {
      _ <- engine.createIndex(indexName)
      _ <- logIndexStatus("was created")
      meta = Meta(epochAsLong)
      eM <- engineMeta
      _ <- eM.upsert(meta)
    } yield meta

  private def logIndexStatus(status: String): F[Unit] =
    L.info(s"Index(${indexName}) ${status}")
}
