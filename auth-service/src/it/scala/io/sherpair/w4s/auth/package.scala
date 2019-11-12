package io.sherpair.w4s

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS

import scala.concurrent.ExecutionContext.global

import cats.effect.{Blocker, ContextShift, IO, Timer}
import com.dimafeng.testcontainers.GenericContainer
import doobie.scalatest.IOChecker
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.noop.NoOpLogger
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.repository.doobie.DoobieRepository
import io.sherpair.w4s.domain.Logger
import org.scalatest.{BeforeAndAfterAll, EitherValues, Matchers, OptionValues, PrivateMethodTester, WordSpec}
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy

package object auth {

  trait AuthSpec
    extends WordSpec
      with BeforeAndAfterAll
      with Matchers
      with EitherValues
      with OptionValues
      with PrivateMethodTester {

    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)
    implicit val logger: Logger[IO] = NoOpLogger.impl[IO]

    implicit val (authConfig: AuthConfig, container: GenericContainer) = initContainer

    override def afterAll: Unit = {
      super.afterAll
      container.stop
    }

    override def beforeAll: Unit = {
      super.beforeAll
      DoobieRepository[IO].use(_.init).unsafeRunSync
    }

    def initContainer: (AuthConfig, Any) = {
      val C: AuthConfig = AuthConfig()
      val db = C.db

      val container = GenericContainer(
        dockerImage = "postgres:alpine",
        exposedPorts = List(db.host.port),
        env = Map(
          "POSTGRES_DB" -> db.name,
          "POSTGRES_USER" -> db.user,
          "POSTGRES_PASSWORD" -> new String(db.secret, StandardCharsets.UTF_8)
        ),
        waitStrategy = new LogMessageWaitStrategy()
          .withRegEx(".*database system is ready to accept connections.*\\s")
          .withTimes(2)
          .withStartupTimeout(Duration.of(60, SECONDS))
      )

      container.start

      val host = db.host.copy(port = container.container.getMappedPort(db.host.port))
      (C.copy(db = db.copy(host = host)), container)
    }
  }

  trait SqlSpec extends AuthSpec with IOChecker {
    lazy val transactor = initTransactor

    def initTransactor: Transactor[IO] = {
      val db = authConfig.db
      Transactor.fromDriverManager[IO](
        db.driver, db.url, db.user, new String(db.secret, StandardCharsets.UTF_8),
        Blocker.liftExecutionContext(ExecutionContexts.synchronous)
      )
    }
  }
}
