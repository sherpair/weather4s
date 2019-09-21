package io.sherpair.w4s.geo

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.sherpair.w4s.config.Service
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.engine.elastic.ElasticEngine
import io.sherpair.w4s.geo.config.Configuration
import org.slf4j.LoggerFactory

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    implicit val service: Service = Service("Geo")
    implicit val configuration: Configuration = Configuration()
    implicit val engine: Engine[IO] = ElasticEngine[IO](configuration.engine)
    implicit val logger: Logger[IO] = Slf4jLogger.getLogger

    new Resources[IO].describe
      .use(_ => IO.never)
      .attempt
      .map(_.fold(exitWithError(_), (_: Unit) => ExitCode.Success))
  }

  def exitWithError(throwable: Throwable): ExitCode = {
    LoggerFactory.getLogger("GeoServer").error("\n\nFatal GeoServer Error. Exiting.\n", throwable)
    ExitCode.Error
  }
}
