package io.sherpair.w4s.auth.repository

import fs2.Stream
import io.sherpair.w4s.auth.domain.Record

trait RepositoryOps[F[_], K, R <: Record[K]] {

  /* Test-only */
  def count: F[Int]

  def delete(id: K): F[Int]

  def empty: F[Int]

  def find(id: K): F[Option[R]]

  def insert(record: R): F[R]

  def list: Stream[F, R]

  def subset(order: String, offset: Long, limit: Long): Stream[F, R]

  def update(record: R): F[Int]
}
