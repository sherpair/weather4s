package io.sherpair.w4s.auth.repository

import io.sherpair.w4s.auth.domain.User

trait RepositoryUserOps[F[_]] extends RepositoryOps[F, Long, User] {

  def delete(fieldId: String, fieldVal: String): F[Int]

  def login(fieldId: String, fieldVal: String, password: String): F[Option[User]]
}
