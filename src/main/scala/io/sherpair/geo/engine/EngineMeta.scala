package io.sherpair.geo.engine

import io.sherpair.geo.domain.Meta

trait EngineMeta[F[_]] {

  val indexName: String = "meta"

  def getById: F[Meta]

  def upsert(meta: Meta): F[String]
}
