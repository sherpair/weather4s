package io.sherpair.geo.domain

import scala.util.Success

import com.sksamuel.elastic4s.{Hit, HitReader}
import com.sksamuel.elastic4s.ElasticApi.indexInto
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexRequest

case class Settings(lastCacheRenewal: Long = epochAsLong) extends AnyVal

object Settings {

  val indexName: String = "settings"

  val idSettings = "0"
  val lastEngineUpdate = "lastEngineUpdate"

  val mapping: String =
    s"""{
       | "mappings": {
       |   "properties": {
       |      "${lastEngineUpdate}": { "type": "long" }
       |    }
       |  }
       |}""".stripMargin

  implicit val HitReader: HitReader[Settings] = (hit: Hit) =>
    Success(
      new Settings(
        hit.sourceField(lastEngineUpdate).toString.toLong
      )
    )

  def decodeFromElastic(response: GetResponse): Settings = response.to[Settings]

  def encodeForElastic(indexName: String, settings: Settings): IndexRequest =
    indexInto(indexName)
      .fields(
        lastEngineUpdate -> settings.lastCacheRenewal
      )
      .id(idSettings)
}
