package io.sherpair.geo.engine.elastic

import cats.effect.Async
import cats.syntax.functor._
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.requests.get.GetRequest
import io.circe.{Decoder, Encoder}
import io.circe.derivation._
import io.sherpair.geo.domain.Meta
import io.sherpair.geo.engine.EngineMeta

class ElasticEngineMeta[F[_] : Async] private[elastic] (elasticClient: ElasticClient) extends EngineMeta[F] {

  private val id = "0"

  implicit val decoder: Decoder[Meta] = deriveDecoder[Meta]
  implicit val encoder: Encoder[Meta] = deriveEncoder[Meta]

  def getById: F[Meta] =
    for {
      response <- elasticClient.execute(GetRequest(indexName, id)).lift
    } yield response.result.to[Meta]

  def upsert(meta: Meta): F[String] =
    for {
      response <- elasticClient.execute(update(id) in indexName docAsUpsert meta).lift
    } yield response.result.result
}


