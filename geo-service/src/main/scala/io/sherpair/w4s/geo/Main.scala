package io.sherpair.w4s.geo

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.sherpair.w4s.domain.Logger
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.engine.elastic.ElasticEngine
import io.sherpair.w4s.geo.config.GeoConfig
import org.slf4j.LoggerFactory

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    implicit val configuration: GeoConfig = GeoConfig()
    implicit val engine: Engine[IO] = ElasticEngine[IO]
    implicit val logger: Logger[IO] = Slf4jLogger.getLogger

    Resources[IO]
      .use(_ => IO.never)
      .attempt
      .map(_.fold(exitWithError(_), (_: Unit) => ExitCode.Success))
  }

  def exitWithError(throwable: Throwable): ExitCode = {
    LoggerFactory.getLogger("Geo-Service").error("\n\nFatal Geo-Service Error. Exiting.\n", throwable)
    ExitCode.Error
  }
}
