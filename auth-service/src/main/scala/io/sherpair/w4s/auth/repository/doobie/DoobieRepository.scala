package io.sherpair.w4s.auth.repository.doobie

import java.nio.charset.StandardCharsets

import scala.concurrent.duration._

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync, Timer}
import cats.syntax.apply._
import cats.syntax.flatMap._
import doobie.FC
import doobie.hikari.HikariTransactor
import doobie.syntax.connectionio._
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.repository.{Repository, RepositoryTokenOps, RepositoryUserOps}
import io.sherpair.w4s.domain.{Logger, W4sError}
import org.flywaydb.core.Flyway

class DoobieRepository[F[_]] (
    transactor: Transactor[F])(implicit C: AuthConfig, L: Logger[F], S: Sync[F], T: Timer[F]
) extends Repository[F] {

  override def healthCheck(attempts: Int, interval: FiniteDuration): F[(Int, String)] =
    FC.isValid(interval.toSeconds.toInt).transact(transactor).ifM(
      S.delay((C.healthAttemptsDB - attempts + 1, "green")),
      if (attempts > 0) T.sleep(interval) *> healthCheck(attempts - 1, interval)
      else S.raiseError[(Int, String)](
        W4sError(s"Health check of DB connection failed after ${C.healthAttemptsDB} attempts")
      )
    )

  override val init: F[Unit] = initialHealthCheck >> migrate

  override val tokenRepositoryOps: F[RepositoryTokenOps[F]] = DoobieRepositoryTokenOps[F](transactor)

  override val userRepositoryOps: F[RepositoryUserOps[F]] = DoobieRepositoryUserOps[F](transactor)

  private lazy val initialHealthCheck: F[Unit] =
    healthCheck(C.healthAttemptsDB, C.healthIntervalDB) >>= { result =>
      val HC = s"Health check successful after ${result._1} ${if (result._1 == 1) "attempt" else "attempts"}"
      L.info(s"${HC}\nStatus of DB connection is ${result._2}")
    }

  private lazy val migrate: F[Unit] =
    S.delay {
      Flyway.configure.dataSource(C.db.url, C.db.user, new String(C.db.secret, StandardCharsets.UTF_8))
        .load.migrate
    } >>= {
      migrations => L.info(s"Applied ${migrations} database migrations")
    }
}

object DoobieRepository {

  def apply[F[_]: Async: ContextShift: Timer](implicit C: AuthConfig, L: Logger[F]): Resource[F, Repository[F]] =
    for {
      connectEC <- ExecutionContexts.fixedThreadPool(C.db.connectionPool)
      db = C.db
      blockerEC <- ExecutionContexts.cachedThreadPool
      transactor: HikariTransactor[F] <- HikariTransactor.newHikariTransactor[F](
        db.driver,
        db.url,
        db.user,
        new String(db.secret, StandardCharsets.UTF_8),
        connectEC,
        Blocker.liftExecutionContext(blockerEC))

      repository <- Resource.liftF(Async[F]delay(new DoobieRepository[F](transactor)))
    }
    yield repository
}
