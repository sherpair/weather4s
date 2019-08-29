package io.sherpair.geo.engine

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.domain._

private[engine] class EngineOpsMeta[F[_]](implicit engine: Engine[F], L: Logger[F], S: Sync[F]) {

  def createIndexIfNotExists: F[EngineMeta] =
    engine.indexExists(EngineMeta.indexName).ifM(firstEngineMetaLoad, initialiseEngineMeta)

  def loadEngineMeta: F[EngineMeta] =
    for {
      response <- engine.getById(EngineMeta.indexName, EngineMeta.id)
      engineMeta <- S.delay(EngineMeta.decodeFromElastic(response))
    } yield engineMeta

  private def firstEngineMetaLoad: F[EngineMeta] =
    for {
      _ <- logIndexStatus("already exists")
      engineMeta <- loadEngineMeta
      _ <- L.info(s"Last Engine update at(${toIsoDate(engineMeta.lastEngineUpdate)})")
    } yield engineMeta

  private def initialiseEngineMeta: F[EngineMeta] =
    for {
      _ <- engine.createIndex(EngineMeta.indexName, EngineMeta.mapping)
      _ <- logIndexStatus("was created")
      engineMeta = EngineMeta(epochAsLong)
      _ <- engine.add(EngineMeta.encodeForElastic(EngineMeta.indexName, engineMeta))
    } yield engineMeta

  private def logIndexStatus(status: String): F[Unit] =
    L.info(s"Index(${EngineMeta.indexName}) ${status}")
}
