package io.sherpair.geo

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.sherpair.geo.algebra.Engine
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.engine.ElasticEngine
import io.sherpair.geo.infrastructure.Resources
import org.slf4j.LoggerFactory

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    implicit val configuration: Configuration = Configuration()
    implicit val engine: Engine[IO] = ElasticEngine[IO]
    implicit val logger: Logger[IO] = Slf4jLogger.getLogger

    new Resources[IO].describe
      .use(_ => IO.never)
      .attempt
      .map(_.fold(exitWithError(_), _ => ExitCode.Success))
  }

  def exitWithError(throwable: Throwable): ExitCode = {
    LoggerFactory.getLogger("GeoServer").error("\n\nFatal GeoServer Error. Exiting.\n", throwable)
    ExitCode.Error
  }
}
