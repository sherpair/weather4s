package io.sherpair.w4s.engine.elastic

import scala.reflect.ClassTag

import cats.effect.Async
import cats.syntax.functor._
import cats.syntax.option._
import com.sksamuel.elastic4s.{AggReader, ElasticApi, ElasticClient, HitReader, Indexable}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.requests.bulk.BulkResponseItem
import com.sksamuel.elastic4s.requests.get.GetRequest
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import io.sherpair.w4s.config.Engine
import io.sherpair.w4s.config.Engine.defaultWindowSize
import io.sherpair.w4s.domain.BulkError
import io.sherpair.w4s.engine.EngineIndex

class ElasticEngineIndex[F[_]: Async, T: ClassTag: AggReader: HitReader: Indexable] private[elastic] (
    elasticClient: ElasticClient, indexName: String, f: T => String, E: Engine
) extends EngineIndex[F, T] {

  private val MaxWindowSize: Int = 10000

  override def count: F[Long] =
    for {
      response <- elasticClient.execute(ElasticApi.count(indexName)).lift
    } yield response.result.count

  // Test-only. Not used by the app.
  override def getById(id: String): F[Option[T]] =
    for {
      response <- elasticClient.execute(GetRequest(indexName, id)).lift
    } yield if (response.result.exists) response.result.to[T].some else None

  /*
   * 0 < windowSize param <= MaxWindowSize
   */
  override def loadAll(sortBy: Option[Seq[String]], windowSize: Int = defaultWindowSize(E)): F[List[T]] = {
    val _windowSize = 1.max(MaxWindowSize.min(windowSize))
    val sorts: Seq[FieldSort] = sortBy.map(_.map(fieldSort(_))).getOrElse(Seq.empty)

    for {
      response <- elasticClient.execute(search(indexName).query(matchAllQuery()).sortBy(sorts) size (_windowSize)).lift
    } yield response.result.to[T].toList
  }

  override def saveAll(documents: List[T]): F[List[BulkError]] =
    for {
      response <- elasticClient
        .execute(bulk {
          for (document <- documents) yield (update(f(document)) in indexName).docAsUpsert(document)
        })
        .lift
    } yield bulkFailuresIfAny(response.result.failures)

  override def upsert(document: T): F[String] =
    for {
      response <- elasticClient.execute((update(f(document)) in indexName).docAsUpsert(document)).lift
    } yield response.result.result

  private def bulkFailuresIfAny(failures: Seq[BulkResponseItem]): List[BulkError] =
    failures.foldLeft(List[BulkError]())((list, bri) => BulkError(bri.id, bri.error.map(_.reason).getOrElse("Generic Error")) :: list)
}
