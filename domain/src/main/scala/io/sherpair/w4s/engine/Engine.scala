package io.sherpair.w4s.engine

import scala.reflect.ClassTag

import io.circe.{Decoder, Encoder}

trait Engine[F[_]] {

  def close: F[Unit]

  def createIndex(name: String, jsonMapping: Option[String] = None): F[Unit]

  def engineIndex[T: ClassTag: Decoder: Encoder](indexName: String, f: T => String): EngineIndex[F, T]

  def execUnderGlobalLock[T](f: => F[T]): F[T]

  def healthCheck: F[(Int, String)]

  def indexExists(name: String): F[Boolean]
}
