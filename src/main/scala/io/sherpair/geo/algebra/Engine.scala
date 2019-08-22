package io.sherpair.geo.algebra

import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.searches.SearchResponse

trait Engine[F[_]] {

  def init: F[String]
  def close: F[Unit]

  def addAll(indexRequests: Seq[IndexRequest]): F[Option[String]]

  def count(indexName: String): F[Long]

  def createIndex(name: String, jsonMapping: String): F[Unit]

  def execUnderGlobalLock[T](f: => F[T]): F[T]

  def indexExists(name: String): F[Boolean]

  /*
   * 0 < size param <= 10,000
   */
  // scalastyle:off magic.number
  def queryAll(indexName: String, sortBy: Option[Seq[String]] = None, size: Int = 1000): F[SearchResponse]
  // scalastyle:on magic.number
}
