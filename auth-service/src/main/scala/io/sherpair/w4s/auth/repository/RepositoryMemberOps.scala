package io.sherpair.w4s.auth.repository

import io.sherpair.w4s.auth.domain.{Member, SignupRequest, UpdateRequest}
import tsec.passwordhashers.PasswordHash

trait RepositoryMemberOps[F[_]] extends RepositoryOps[F, Long, Member] {

  def delete(fieldId: String, fieldVal: String): F[Int]

  def disable(id: Long): F[Int]

  def enable(id: Long): F[Int]

  def find(fieldId: String, fieldVal: String): F[Option[Member]]

  def findForSignin(fieldId: String, fieldVal: String): F[Option[(Member, String)]]

  def insert[A](signupRequest: SignupRequest, secret: PasswordHash[A]): F[Member]

  def update(id: Long, updateRequest: UpdateRequest): F[Int]
}
