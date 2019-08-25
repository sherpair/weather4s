package io.sherpair.geo.domain

import scala.util.Success

import com.sksamuel.elastic4s.{Hit, HitReader}
import com.sksamuel.elastic4s.ElasticApi.indexInto
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import io.chrisdavenport.log4cats.Logger

case class EngineMeta(lastEngineUpdate: Long = epochAsLong) extends AnyVal

object EngineMeta {

  val indexName: String = "meta"

  val id = "0"
  val lastEngineUpdate = "lastEngineUpdate"

  val mapping: String =
    s"""{
       | "mappings": {
       |   "properties": {
       |      "${lastEngineUpdate}": { "type": "long" }
       |    }
       |  }
       |}""".stripMargin

  implicit val HitReader: HitReader[EngineMeta] = (hit: Hit) =>
    Success(
      new EngineMeta(
        hit.sourceField(lastEngineUpdate).toString.toLong
      )
    )

  def decodeFromElastic(response: GetResponse): EngineMeta = response.to[EngineMeta]

  def encodeForElastic(indexName: String, engineMeta: EngineMeta): IndexRequest =
    indexInto(indexName)
      .fields(
        lastEngineUpdate -> engineMeta.lastEngineUpdate
      )
      .id(id)

  def logLastEngineUpdate[F[_]: Logger](lastEngineUpdate: Long): F[Unit] =
    Logger[F].info(s"Last Engine update at(${toIsoDate(lastEngineUpdate)})")
}
