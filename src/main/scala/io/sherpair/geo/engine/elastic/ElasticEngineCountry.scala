package io.sherpair.geo.engine.elastic

import cats.effect.Async
import cats.syntax.functor._
import cats.syntax.option._
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.requests.bulk.BulkResponseItem
import com.sksamuel.elastic4s.requests.get.GetRequest
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.config.Configuration.defaultWindowSize
import io.sherpair.geo.domain.{BulkError, Countries, Country}
import io.sherpair.geo.engine.EngineCountry
import io.sherpair.geo.engine.EngineCountry.indexName

class ElasticEngineCountry[F[_]: Async] private[elastic] (elasticClient: ElasticClient)(implicit C: Configuration)
  extends EngineCountry[F] {

  // scalastyle:off magic.number
  private val MaxWindowSize: Int = 10000
  // scalastyle:on magic.number

  // Test-only. Not used by the app.
  override def getById(id: String): F[Option[Country]] =
    for {
      response <- elasticClient.execute(GetRequest(indexName, id)).lift
    } yield if (response.result.exists) response.result.to[Country].some else None

  /*
   * 0 < windowSize param <= MaxWindowSize
   */
  override def loadAll(sortBy: Option[Seq[String]], windowSize: Int = defaultWindowSize(C)): F[Countries] = {
    val _windowSize = 1.max(MaxWindowSize.min(windowSize))
    val sorts: Seq[FieldSort] = sortBy.map(_.map(fieldSort(_))).getOrElse(Seq.empty)

    for {
      response <- elasticClient.execute(search(indexName).query(matchAllQuery()).sortBy(sorts) size (_windowSize)).lift
    } yield response.result.to[Country].toList
  }

  override def saveAll(countries: Countries): F[List[BulkError]] =
    for {
      response <- elasticClient
        .execute(bulk {
          for (country <- countries) yield (update(country.code) in indexName).docAsUpsert(country)
        })
        .lift
    } yield bulkFailuresIfAny(response.result.failures)

  // Test-only. Not used by the app.
  override def upsert(country: Country): F[String] =
    for {
      response <- elasticClient.execute((update(country.code) in indexName).docAsUpsert(country)).lift
    } yield response.result.result

  private def bulkFailuresIfAny(failures: Seq[BulkResponseItem]): List[BulkError] =
    failures.foldLeft(List[BulkError]())((list, bri) => BulkError(bri.id, bri.error.map(_.reason).getOrElse("Generic Error")) :: list)
}
