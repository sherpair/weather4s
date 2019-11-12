package io.sherpair.w4s.auth.repository

import io.sherpair.w4s.auth.domain.{SignupRequest, UpdateRequest, User}
import tsec.passwordhashers.PasswordHash

trait RepositoryUserOps[F[_]] extends RepositoryOps[F, Long, User] {

  def delete(fieldId: String, fieldVal: String): F[Int]

  def disable(id: Long): F[Int]

  def enable(id: Long): F[Int]

  def find(fieldId: String, fieldVal: String): F[Option[User]]

  def findForSignin(fieldId: String, fieldVal: String): F[Option[(User, String)]]

  def insert[A](signupRequest: SignupRequest, secret: PasswordHash[A]): F[User]

  def update(id: Long, updateRequest: UpdateRequest): F[Int]
}
