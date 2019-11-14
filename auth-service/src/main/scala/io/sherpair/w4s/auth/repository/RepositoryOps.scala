package io.sherpair.w4s.auth.repository

import fs2.Stream

trait RepositoryOps[F[_], K, R] {

  /* Test-only */
  def count: F[Long]

  def delete(id: K): F[Int]

  def empty: F[Int]

  def find(id: K): F[Option[R]]

  def list: Stream[F, R]

  def subset(order: String, offset: Long, limit: Long): Stream[F, R]
}
