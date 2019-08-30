package io.sherpair.geo.engine

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.domain._

private[engine] class EngineOpsMeta[F[_]](implicit engine: Engine[F], L: Logger[F], S: Sync[F]) {

  private val engineMeta = engine.engineMeta

  def createIndexIfNotExists: F[Meta] =
    engine.indexExists(engineMeta.indexName).ifM(firstMetaLoad, initialiseMeta)

  def loadMeta: F[Meta] = engineMeta.getById

  private def firstMetaLoad: F[Meta] =
    for {
      _ <- logIndexStatus("already exists")
      meta <- loadMeta
      _ <- L.info(s"Last Engine update at(${toIsoDate(meta.lastEngineUpdate)})")
    } yield meta

  private def initialiseMeta: F[Meta] =
    for {
      _ <- engine.createIndex(engineMeta.indexName)
      _ <- logIndexStatus("was created")
      meta = Meta(epochAsLong)
      _ <- engineMeta.upsert(meta)
    } yield meta

  private def logIndexStatus(status: String): F[Unit] =
    L.info(s"Index(${engineMeta.indexName}) ${status}")
}
