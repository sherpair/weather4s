package io.sherpair.w4s.auth.repository

import io.sherpair.w4s.auth.domain.{Member, SignupRequest, UpdateRequest}

trait RepositoryMemberOps[F[_]] extends RepositoryOps[F, Long, Member] {

  def delete(fieldId: String, fieldVal: String): F[Int]

  def disable(id: Long): F[Int]

  def enable(id: Long): F[Int]

  def find(fieldId: String, fieldVal: String): F[Option[Member]]

  def findWithSecret(fieldId: String, fieldVal: String): F[Option[(Member, String)]]

  def insert(signupRequest: SignupRequest): F[Member]

  def update(id: Long, email: String): F[Option[Member]]

  def update(id: Long, secret: Array[Byte]): F[Int]

  def update(id: Long, updateRequest: UpdateRequest): F[Option[Member]]
}
