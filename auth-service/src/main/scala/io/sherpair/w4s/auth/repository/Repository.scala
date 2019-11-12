package io.sherpair.w4s.auth.repository

import scala.concurrent.duration.FiniteDuration

trait Repository[F[_]] {

  def healthCheck(attempts: Int, interval: FiniteDuration): F[(Int, String)]

  val init: F[Unit]

  val tokenRepositoryOps: F[RepositoryTokenOps[F]]

  val userRepositoryOps: F[RepositoryUserOps[F]]
}
