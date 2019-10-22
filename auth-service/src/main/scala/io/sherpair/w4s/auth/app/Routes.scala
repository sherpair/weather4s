package io.sherpair.w4s.auth.app

import cats.effect.{ConcurrentEffect => CE, Resource}
import cats.syntax.applicative._
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.repository.{Repository, RepositoryUserOps}
import io.sherpair.w4s.domain.Logger
import org.http4s.HttpRoutes

object Routes {

  def apply[F[_]: CE](implicit C: AuthConfig, L: Logger[F], R: Repository[F]): Resource[F, Seq[HttpRoutes[F]]] =
    for {
      implicit0(repositoryUserOps: RepositoryUserOps[F]) <- Resource.liftF(R.userRepositoryOps)
      routes <- Resource.liftF(
        Seq(
          new Monitoring[F].routes,
          new UserApp[F].routes
        ).pure[F]
      )
    }
    yield routes
}
