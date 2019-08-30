package io.sherpair.geo.engine.elastic

import cats.effect.Async
import cats.syntax.functor._
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

class ElasticEngineCountry[F[_]: Async] private[elastic] (elasticClient: ElasticClient)(implicit C: Configuration)
    extends EngineCountry[F] {

  def addInBulk(countries: Countries): F[List[BulkError]] =
    for {
      response <- elasticClient
        .execute(bulk {
          for (country <- countries) yield (update(country.code) in indexName).docAsUpsert(country)
        })
        .lift
    } yield getBulkFailuresIfAny(response.result.failures)

  def getById(id: String): F[Country] =
    for {
      response <- elasticClient.execute(GetRequest(indexName, id)).lift
    } yield response.result.to[Country]

  def jsonMapping: String =
    """{
      | "mappings": {
      |   "properties": {
      |      "code":    { "type": "text" },
      |      "name":    { "type": "text" },
      |      "updated": { "type": "long" }
      |    }
      |  }
      |}""".stripMargin

  /*
   * 0 < windowSize param <= MaxWindowSize
   */
  def loadAll(sortBy: Option[Seq[String]], windowSize: Int = defaultWindowSize(C)): F[Countries] = {
    val _windowSize = Math.max(1, Math.min(MaxWindowSize, windowSize))
    val sorts: Seq[FieldSort] = sortBy.map(_.map(fieldSort(_))).getOrElse(Seq.empty)

    for {
      response <- elasticClient.execute(search(indexName).query(matchAllQuery()).sortBy(sorts) size (_windowSize)).lift
    } yield response.result.to[Country].toList
  }

  def upsert(country: Country): F[String] =
    for {
      response <- elasticClient.execute((update(country.code) in indexName).docAsUpsert(country)).lift
    } yield response.result.result

  private def getBulkFailuresIfAny(failures: Seq[BulkResponseItem]): List[BulkError] =
    failures.foldLeft(List[BulkError]())((list, bri) => BulkError(bri.id, bri.error.map(_.reason).getOrElse("Generic Error")) :: list)
}
