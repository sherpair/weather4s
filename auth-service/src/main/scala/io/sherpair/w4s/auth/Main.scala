package io.sherpair.w4s.auth

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.option._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.sherpair.w4s.auth.config.{AuthConfig, Smtp, SmtpEnv, Transporter}
import io.sherpair.w4s.auth.repository.doobie.DoobieRepository
import io.sherpair.w4s.domain.Logger
import org.slf4j.LoggerFactory
import tsec.passwordhashers.jca.SCrypt

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    implicit val configuration: AuthConfig = AuthConfig().copy(smtp = Smtp(smtpEnv).some)
    implicit val logger: Logger[IO] = Slf4jLogger.getLogger

    CallGraph[IO, SCrypt](SCrypt, DoobieRepository[IO])
      .use(_ => IO.never)
      .attempt
      .map(_.fold(exitWithError(_), (_: Unit) => ExitCode.Success))
  }

  private def envVar(name: String): String = sys.env.get(name).getOrElse(envVarUnset(name))

  private def envVarUnset(name: String): String =
    throw new NoSuchElementException(s"Env var '${name}' is not set but it's mandatory. Aborting...")

  def exitWithError(throwable: Throwable): ExitCode = {
    LoggerFactory.getLogger("Auth-Service").error("\n\nFatal Auth-Service Error. Exiting.\n", throwable)
    ExitCode.Error
  }

  def smtpEnv: SmtpEnv = SmtpEnv(
    envVar("W4S_AUTH_SMTP_ADDRESS"),
    envVar("W4S_AUTH_SMTP_PORT"),
    envVar("W4S_AUTH_SMTP_USER"),
    envVar("W4S_AUTH_SMTP_SECRET"),
    new Transporter
  )
}
