package io.sherpair.w4s.loader

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.sherpair.w4s.domain.Logger
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.engine.elastic.ElasticEngine
import io.sherpair.w4s.loader.config.LoaderConfig
import io.sherpair.w4s.loader.domain.LoaderContext
import org.slf4j.LoggerFactory

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    implicit val configuration: LoaderConfig = LoaderConfig()
    implicit val engine: Engine[IO] = ElasticEngine[IO]
    implicit val logger: Logger[IO] = Slf4jLogger.getLogger

    implicit val loaderContext: LoaderContext[IO] = {
      val es = Executors.newSingleThreadExecutor(loaderThread(_))
      val ec = ExecutionContext.fromExecutorService(es)
      LoaderContext[IO](IO.contextShift(ec), ec)
    }

    Resources[IO]
      .use(_ => IO.never)
      .attempt
      .map(_.fold(exitWithError(_), (_: Unit) => ExitCode.Success))
  }

  def loaderThread(r: Runnable): Thread = {
    val thread = new Thread(r, "loader-thp")
    // JVM waits the thread completion before exiting
    thread.setDaemon(false)
    thread
  }

  def exitWithError(throwable: Throwable): ExitCode = {
    LoggerFactory.getLogger("Loader-Service").error("\n\nFatal Loader-Service Error. Exiting.\n", throwable)
    ExitCode.Error
  }
}
