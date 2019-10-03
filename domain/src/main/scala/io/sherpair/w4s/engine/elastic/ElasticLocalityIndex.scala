package io.sherpair.w4s.engine.elastic

import scala.util.{Failure, Success}

import cats.effect.Async
import cats.syntax.functor._
import cats.syntax.option._
import com.sksamuel.elastic4s.{AggReader, ElasticApi, ElasticClient, Hit, HitReader, Indexable}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.requests.bulk.BulkResponseItem
import com.sksamuel.elastic4s.requests.get.GetRequest
import io.circe.Encoder
import io.circe.jawn.decode
import io.sherpair.w4s.domain.{jsonPrinter, unit, BulkError, Country, Localities, Locality}
import io.sherpair.w4s.engine.LocalityIndex

class ElasticLocalityIndex[F[_]: Async] private[elastic] (elasticClient: ElasticClient) extends LocalityIndex[F] {

  implicit val aggReader: AggReader[Locality] = (json: String) => decode[Locality](json).fold(Failure(_), Success(_))
  implicit val hitReader: HitReader[Locality] = (hit: Hit) => decode[Locality](hit.sourceAsString).fold(Failure(_), Success(_))
  implicit val indexable: Indexable[Locality] = (l: Locality) => jsonPrinter(Encoder[Locality].apply(l))

  def count(country: Country): F[Long] =
    elasticClient.execute(ElasticApi.count(country.code)).lift.map(_.result.count)

  // Test-only. Not used by the app.
  def getById(country: Country, id: String): F[Option[Locality]] =
    for {
      response <- elasticClient.execute(GetRequest(country.code, id)).lift
    } yield if (response.result.exists) response.result.to[Locality].some else None

  def delete(country: Country): F[Unit] =
    elasticClient.execute(deleteIndex(country.code)).lift.map(_ => unit)

  def saveAll(country: Country, localities: Localities): F[List[BulkError]] =
    for {
      response <- elasticClient
        .execute(bulk {
          for (locality <- localities) yield (update(locality.geoId) in country.code).docAsUpsert(locality)
        })
        .lift
    } yield bulkFailuresIfAny(response.result.failures)

  private def bulkFailuresIfAny(failures: Seq[BulkResponseItem]): List[BulkError] =
    failures.foldLeft(List[BulkError]())(
      (list, bri) => BulkError(bri.id, bri.error.map(_.reason).getOrElse("Generic Error")) :: list)
}
