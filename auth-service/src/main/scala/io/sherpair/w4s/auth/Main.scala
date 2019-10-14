package io.sherpair.w4s.auth

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.repository.doobie.DoobieRepository
import io.sherpair.w4s.domain.Logger
import org.slf4j.LoggerFactory

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    implicit val configuration: AuthConfig = AuthConfig()
    implicit val logger: Logger[IO] = Slf4jLogger.getLogger

    Resources[IO](DoobieRepository[IO])
      .use(_ => IO.never)
      .attempt
      .map(_.fold(exitWithError(_), (_: Unit) => ExitCode.Success))
  }

  def exitWithError(throwable: Throwable): ExitCode = {
    LoggerFactory.getLogger("Auth-Service").error("\n\nFatal Auth-Service Error. Exiting.\n", throwable)
    ExitCode.Error
  }
}
