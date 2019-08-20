package io.sherpair.geo.infrastructure

import cats.FlatMap
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.sksamuel.elastic4s.ElasticDsl.emptyMapping
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.algebra.Engine
import io.sherpair.geo.config.Configuration

class EngineCriticalOps[F[_]: FlatMap: Logger](implicit config: Configuration, engine: Engine[F]) {
  def init: F[Unit] =
    for {
      status <- engine.init
      _ <- logEngineStatus(status)
      created <- engine.createIndexIfNotExists(indexCountries, emptyMapping)
      _ <- Logger[F].info(s"Index(${indexCountries}) ${isNewIndex(created)} in ES cluster(${config.elasticSearch.cluster.name})")
    } yield ()

  def close: F[Unit] =
    Logger[F].info(s"Closing connection with ES cluster(${config.elasticSearch.cluster.name})") *> engine.close

  private def indexCountries: String = "countries"

  private def isNewIndex(created: Boolean): String = if (created) "was created" else "already exists"

  private def logEngineStatus(status: String): F[Unit] = {
    def extractStatus(status: String): String = status.toLowerCase match {
      case "red" => "**** RED!! ****"
      case "yellow" => "** YELLOW **"
      case _ => status
    }

    Logger[F].info(s"Status of ES cluster(${config.elasticSearch.cluster.name}) is ${extractStatus(status)}")
  }
}
