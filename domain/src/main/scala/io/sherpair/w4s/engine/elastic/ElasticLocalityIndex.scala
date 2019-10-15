package io.sherpair.w4s.engine.elastic

import scala.util.{Failure, Success}

import cats.effect.Async
import cats.syntax.functor._
import cats.syntax.option._
import com.sksamuel.elastic4s.{ElasticApi, ElasticClient, Hit, HitReader, Indexes, Response}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.requests.bulk.BulkResponseItem
import com.sksamuel.elastic4s.requests.common.FetchSourceContext
import com.sksamuel.elastic4s.requests.get.GetRequest
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.searches.{SearchRequest, SearchResponse}
import com.sksamuel.elastic4s.requests.searches.suggestion.{CompletionSuggestion, Fuzziness}
import io.circe.{parser, Json}
import io.circe.jawn.decode
import io.sherpair.w4s.config.{Suggestions => Parameters}
import io.sherpair.w4s.domain.{
  unit, BulkError, Country, Localities, Locality, Suggestion, SuggestionMeta, Suggestions
}
import io.sherpair.w4s.engine.LocalityIndex

class ElasticLocalityIndex[F[_]: Async] private[elastic] (elasticClient: ElasticClient) extends LocalityIndex[F] {

  // Not used.
  // implicit val aggReader: AggReader[Locality] =
  //   (json: String) => decode[Locality](json).fold(Failure(_), Success(_))

  implicit val hitReader: HitReader[Locality] =
    (hit: Hit) => decode[Locality](hit.sourceAsString).fold(Failure(_), Success(_))

  // Not used.
  // implicit val indexable: Indexable[Locality] = (l: Locality) => jsonPrinter(Encoder[Locality].apply(l))

  val fetchContext = FetchSourceContext(true, Array("name", "coord", "tz"), Array.empty[String]).some
  val suggestionId = "localities"

  override def count(country: Country): F[Long] =
    elasticClient.execute(ElasticApi.count(country.code)).lift.map(_.result.count)

  // Test-only. Not used by the app.
  override def getById(country: Country, geoId: String): F[Option[Locality]] =
    for {
      response <- elasticClient.execute(GetRequest(country.code, geoId)).lift
    }
    yield if (response.result.exists) response.result.to[Locality].some else None

  override def delete(country: Country): F[Unit] =
    elasticClient.execute(deleteIndex(country.code)).lift.map(_ => unit)

  override def saveAll(country: Country, localities: Localities): F[List[BulkError]] =
    for {
      response <- elasticClient.execute(bulk {
        for (locality <- localities)
          yield IndexRequest(index = country.code, id = locality.geoId.some, source = locality.toJson.some)
      })
      .lift
    }
    yield bulkFailuresIfAny(response.result.failures)

  override def suggest(country: Country, localityTerm: String, parameters: Parameters): F[Suggestions] =
    suggest(country.code, localityTerm, parameters, "suggestion.name")

  override def suggestByAsciiOnly(country: Country, localityTerm: String, parameters: Parameters): F[Suggestions] =
    suggest(country.code, localityTerm, parameters, "asciiOnly")

  private def bulkFailuresIfAny(failures: Seq[BulkResponseItem]): List[BulkError] =
    failures.foldLeft(List[BulkError]())(
      (list, bri) => BulkError(bri.id, bri.error.map(_.reason).getOrElse("Generic Error")) :: list)

  private def suggest(code: String, term: String, parameters: Parameters, field: String): F[Suggestions] =
    elasticClient.execute(
      SearchRequest(
        indexes = Indexes(List(code)),
        fetchContext = fetchContext,
        suggs = List(CompletionSuggestion(
          name = suggestionId,
          fieldname = field,
          analyzer = parameters.analyzer.entryName.some,
          fuzziness = Fuzziness.fromEdits(parameters.fuzziness).some,
          prefix = term.some,
          size = parameters.maxSuggestions.some,
          skipDuplicates = true.some
        ))
      )
    ).lift.map(response => suggestResponse(response))

  private def suggestResponse(response: Response[SearchResponse]): Suggestions =
    (for {
      body <- response.body
      json: Json = parser.parse(body).getOrElse(Json.Null)
      json <- json.hcursor.downField("suggest").downField(suggestionId).downArray.downField("options").focus
      metas <- json.as[List[SuggestionMeta]].toOption
    }
    yield metas.map(meta => meta._source))
      .getOrElse(List.empty[Suggestion])
}
