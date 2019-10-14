package io.sherpair.w4s.auth.repository

import scala.concurrent.duration.FiniteDuration

import cats.effect.Resource

trait Repository[F[_]] {

  def healthCheck(attempts: Int, interval: FiniteDuration): F[(Int, String)]

  val init: Resource[F, Unit]

  def userRepositoryOps: Resource[F, RepositoryUserOps[F]]
}
