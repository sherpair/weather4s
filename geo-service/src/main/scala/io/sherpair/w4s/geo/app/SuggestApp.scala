package io.sherpair.w4s.geo.app

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import io.circe.syntax.EncoderOps
import io.sherpair.w4s.config.{Suggestions => Parameters}
import io.sherpair.w4s.domain.{Analyzer => DefaultAnalyzer, Country, Suggestion}
import io.sherpair.w4s.geo.cache.CacheRef
import io.sherpair.w4s.geo.config.GeoConfig
import io.sherpair.w4s.geo.engine.EngineOps
import io.sherpair.w4s.http.MT
import io.sherpair.w4s.types.Suggestions
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class SuggestApp[F[_]: Sync](
  cacheRef: CacheRef[F], engineOps: EngineOps[F])(implicit C: GeoConfig) extends Http4sDsl[F] {

  implicit val suggestionEncoder: EntityEncoder[F, Suggestion] = jsonEncoderOf[F, Suggestion]

  object Analyzer extends OptionalQueryParamDecoderMatcher[String]("analyzer")
  object Fuzziness extends OptionalQueryParamDecoderMatcher[Int]("fuzziness")
  object MaxSuggestions extends OptionalQueryParamDecoderMatcher[Int]("maxSuggestions")

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "suggest" / id / localityTerm
      :? Analyzer(analyzer)
      +& Fuzziness(fuzziness)
      +& MaxSuggestions(maxSuggestions) =>

    (if (id.length == 2) cacheRef.countryByCode(id.toLowerCase) else cacheRef.countryByName(id)) >>= {
      _.map { country =>
        resolveParameters(country, analyzer, fuzziness, maxSuggestions) match {
          case Left(error) => BadRequest(error)
          case Right(parameters) => Ok(suggest(country, localityTerm, parameters), MT)
        }
      }
      .getOrElse(NotFound(s"Country(${id}) is not known"))
    }
  }

  private def resolveParameters(
      country: Country, analyzerO: Option[String], fuzziness: Option[Int], maxSuggestions: Option[Int]
  ): Either[String, Parameters] =
    analyzerO.flatMap(a => DefaultAnalyzer.withNameOption(a.trim.toLowerCase)) match {
      case None if analyzerO.isDefined => Left(s"(${analyzerO.get}) is not a valid Analyzer for ${country}")
      case analyzer =>
        Right(Parameters(
          analyzer.map(identity).getOrElse(country.analyzer),
          fuzziness.getOrElse(C.suggestions.fuzziness),
          maxSuggestions.getOrElse(C.suggestions.maxSuggestions)
        ))
    }

  private def suggest(country: Country, localityTerm: String, parameters: Parameters): Stream[F, String] = {
    // Ascii control chars (0-31, 127) invalidate the response --> no suggestions
    val suggestions: F[Suggestions] = localityTerm.indexWhere(_ > 127) match {
      case -1 => engineOps.suggestByAsciiOnly(country, localityTerm, parameters)
      case _ => engineOps.suggest(country, localityTerm, parameters)
    }

    Stream.eval(suggestions.map(_.asJson.noSpaces))
  }
}
