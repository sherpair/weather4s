package io.sherpair.geo.engine

import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.{IndexRequest, IndexResponse}
import com.sksamuel.elastic4s.requests.searches.SearchResponse

trait Engine[F[_]] {

  val DefaultWindowSize = 1000
  val MaxWindowSize = 10000

  def close: F[Unit]
  def healthCheck: F[String]

  def add(indexRequest: IndexRequest): F[IndexResponse]

  def addAll(indexRequests: Seq[IndexRequest]): F[Option[String]]

  def count(indexName: String): F[Long]

  def createIndex(name: String, jsonMapping: String): F[Unit]

  def execUnderGlobalLock[T](f: => F[T]): F[T]

  def getById(indexName: String, id: String): F[GetResponse]

  def indexExists(name: String): F[Boolean]

  /*
   * 0 < windowSize param <= MaxWindowSize
   */
  def queryAll(indexName: String, sortBy: Option[Seq[String]] = None, windowSize: Int = DefaultWindowSize): F[SearchResponse]
}
