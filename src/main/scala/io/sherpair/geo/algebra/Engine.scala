package io.sherpair.geo.algebra

import com.sksamuel.elastic4s.requests.mappings.MappingDefinition

trait Engine[F[_]] {
  def init: F[String]
  def close: F[Unit]
  def createIndexIfNotExists(name: String, mapping: MappingDefinition): F[Boolean]
}
