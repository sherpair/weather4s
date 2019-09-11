package io.sherpair.geo.engine.elastic

import cats.effect.Async
import cats.syntax.functor._
import cats.syntax.option._
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.requests.get.GetRequest
import io.sherpair.geo.domain.Meta
import io.sherpair.geo.engine.EngineMeta
import io.sherpair.geo.engine.EngineMeta.indexName

class ElasticEngineMeta[F[_] : Async] private[elastic] (elasticClient: ElasticClient) extends EngineMeta[F] {

  private val id = "0"

  override def getById: F[Option[Meta]] =
    for {
      response <- elasticClient.execute(GetRequest(indexName, id)).lift
    } yield if (response.result.exists) response.result.to[Meta].some else None

  override def upsert(meta: Meta): F[String] =
    for {
      response <- elasticClient.execute(update(id) in indexName docAsUpsert meta).lift
    } yield response.result.result
}


