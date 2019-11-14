package io.sherpair.w4s.auth.repository.doobie

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS

import cats.effect.{Blocker, IO}
import com.dimafeng.testcontainers.GenericContainer
import doobie.scalatest.IOChecker
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.sherpair.w4s.auth.AuthSpec
import io.sherpair.w4s.auth.config.AuthConfig
import org.scalatest.BeforeAndAfterAll
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy

trait TransactorSpec extends AuthSpec with BeforeAndAfterAll with IOChecker {
  lazy val transactor = initTransactor

  implicit val (aC: AuthConfig, container: GenericContainer) = initContainer

  override def afterAll: Unit = {
    super.afterAll
    container.stop
  }

  override def beforeAll: Unit = {
    super.beforeAll
    DoobieRepository[IO].use(_.init).unsafeRunSync
  }

  def initContainer: (AuthConfig, Any) = {
    val C: AuthConfig = AuthConfig().copy(smtp = fakeSmtp)
    val db = C.db

    val container = GenericContainer(
      dockerImage = "postgres:alpine",
      exposedPorts = List(db.host.port),
      env = Map(
        "POSTGRES_DB" -> db.name,
        "POSTGRES_USER" -> db.user,
        "POSTGRES_PASSWORD" -> new String(db.secret, UTF_8)
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

  def initTransactor: Transactor[IO] = {
    val db = aC.db
    Transactor.fromDriverManager[IO](
      db.driver, db.url, db.user, new String(db.secret, UTF_8),
      Blocker.liftExecutionContext(ExecutionContexts.synchronous)
    )
  }
}
