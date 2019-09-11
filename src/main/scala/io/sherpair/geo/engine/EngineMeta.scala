package io.sherpair.geo.engine

import io.sherpair.geo.domain.Meta

trait EngineMeta[F[_]] {

  def getById: F[Option[Meta]]

  def upsert(meta: Meta): F[String]
}

object EngineMeta {

  val indexName: String = "meta"
}
