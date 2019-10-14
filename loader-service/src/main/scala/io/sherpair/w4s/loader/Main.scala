package io.sherpair.w4s.loader

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.sherpair.w4s.domain.Logger
import io.sherpair.w4s.engine.elastic.ElasticEngine
import io.sherpair.w4s.loader.config.LoaderConfig
import org.slf4j.LoggerFactory

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    implicit val configuration: LoaderConfig = LoaderConfig()
    implicit val logger: Logger[IO] = Slf4jLogger.getLogger

    Resources[IO](ElasticEngine[IO])
      .use(_ => IO.never)
      .attempt
      .map(_.fold(exitWithError(_), (_: Unit) => ExitCode.Success))
  }

  def exitWithError(throwable: Throwable): ExitCode = {
    LoggerFactory.getLogger("Loader-Service").error("\n\nFatal Loader-Service Error. Exiting.\n", throwable)
    ExitCode.Error
  }
}
