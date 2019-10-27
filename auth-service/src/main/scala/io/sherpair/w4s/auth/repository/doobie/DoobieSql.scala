package io.sherpair.w4s.auth.repository.doobie

import doobie.{ConnectionIO, Query0, Update0}
import io.sherpair.w4s.auth.domain.Record

private[doobie] trait DoobieSql[K, R <: Record[K]] {

  /* Test-only */
  val countSql: Query0[Int]

  def deleteSql(id: K): Update0

  def deleteSql(fieldId: String, fieldVal: String): Update0

  val emptySql: Update0

  def findSql(id: K): Query0[R]

  def insertSql(record: R): ConnectionIO[R]

  val listSql: Query0[R]

  def loginSql(fieldId: String, fieldVal: String, password: String): Query0[R]

  def subsetSql(order: String, limit: Long, offset: Long): Query0[R]

  def updateSql(record: R): ConnectionIO[Int]
}
