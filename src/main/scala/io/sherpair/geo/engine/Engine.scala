package io.sherpair.geo.engine

trait Engine[F[_]] {

  def close: F[Unit]

  def count(indexName: String): F[Long]

  def createIndex(name: String, jsonMapping: Option[String] = None): F[Unit]

  def engineCountry: EngineCountry[F]

  def engineMeta: EngineMeta[F]

  def execUnderGlobalLock[T](f: => F[T]): F[T]

  def healthCheck: F[String]

  def indexExists(name: String): F[Boolean]
}
