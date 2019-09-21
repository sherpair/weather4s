package io.sherpair.w4s.engine

import io.sherpair.w4s.domain.BulkError

trait EngineIndex[F[_], T] {

  def count: F[Long]

  // Test-only. Not used by the app.
  def getById(id: String): F[Option[T]]

  /*
   * 0 < windowSize param <= MaxWindowSize  (10,000)
   */
  def loadAll(sortBy: Option[Seq[String]] = None, windowSize: Int = EngineIndex.defaultWindowSize): F[List[T]]

  def saveAll(documents: List[T]): F[List[BulkError]]

  def upsert(document: T): F[String]
}

object EngineIndex {

  val bulkErrorMessage: String = "Got one or more errors while storing countries to the engine:\n"

  val defaultWindowSize: Int = 1000
}
