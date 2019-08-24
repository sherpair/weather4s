package io.sherpair.geo.engine

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.domain._

private[engine] class EngineOpsSettings[F[_]](implicit engine: Engine[F], L: Logger[F], S: Sync[F]) {

  def createIndexIfNotExists: F[Settings] =
    for {
      indexExists <- engine.indexExists(Settings.indexName)
      settings <- if (indexExists) loadSettings else initialiseSettings
    } yield settings

  private def initialiseSettings: F[Settings] =
    for {
      _ <- engine.createIndex(Settings.indexName, Settings.mapping)
      _ <- logIndexStatus("was created")
      settings = Settings(epochAsLong)
      _ <- engine.add(Settings.encodeForElastic(Settings.indexName, settings))
    } yield settings

  private def loadSettings: F[Settings] =
    for {
      _ <- logIndexStatus("already exists")
      response <- engine.getById(Settings.indexName, Settings.idSettings)
      settings <- S.delay(Settings.decodeFromElastic(response))
      _ <- L.info(s"Last Engine update(${toIsoDate(settings.lastCacheRenewal)})")
    } yield settings

  private def logIndexStatus(status: String): F[Unit] =
    L.info(s"Index(${Settings.indexName}) ${status}")
}
