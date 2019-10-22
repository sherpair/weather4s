package io.sherpair.w4s.auth.repository

import io.sherpair.w4s.auth.domain.Record

trait RepositoryOps[F[_], K, R <: Record[K]] {

  def delete(id: K): Result[F, Int]

  def deleteX(id: K): F[Int]

  def empty: Result[F, Int]

  def emptyX: F[Int]

  def find(id: K): ResultOption[F, K, R]

  def findX(id: K): F[Option[R]]

  def insert(record: R): ResultRecord[F, K, R]

  def insertX(record: R): F[R]

  def list: ResultStream[K, R]

  def subset(order: String, offset: Long, limit: Long): ResultList[F, K, R]

  def subsetX(order: String, offset: Long, limit: Long): F[List[R]]

  def update(record: R): Result[F, Int]

  def updateX(record: R): F[Int]
}
