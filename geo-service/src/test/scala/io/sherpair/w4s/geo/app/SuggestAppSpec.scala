package io.sherpair.w4s.geo.app

import java.net.URLEncoder.encode

import cats.effect.{IO, Sync}
import io.sherpair.w4s.domain.now
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.engine.memory.DataSuggesters
import io.sherpair.w4s.geo.{DataSuggesterMap, GeoSpec, IOengine, IOengineWithDataSuggesters}
import io.sherpair.w4s.geo.cache.{Cache, CacheRef}
import io.sherpair.w4s.geo.engine.EngineOps
import io.sherpair.w4s.types.Suggestions
import org.http4s.{Request, Response, Status}
import org.http4s.Method.GET
import org.http4s.Uri.unsafeFromString
import org.http4s.server.Router
import org.http4s.syntax.kleisli._

class SuggestAppSpec extends GeoSpec with DataSuggesterMap {

  "GET -> /geo/suggest/{id}/{term}" should {
    "return 404 when the resource url is incomplete" in new IOengine {
      val response =
        withSuggestAppRoutes(Request[IO](GET, unsafeFromString(s"${C.root}/suggest/${headTerm}"))).unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  "GET -> /geo/suggest/{id}/{term}" should {
    "return a number of suggestions, according to country and locality term provided" in new IOengineWithDataSuggesters {
      override def dataSuggesterMap: Map[String, DataSuggesters] = _dataSuggesterMap

      val responseIO = withSuggestAppRoutes(
        Request[IO](GET, unsafeFromString(s"${C.root}/suggest/${headCountry}/${headTerm}"))
      )
      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

      val suggestions: Suggestions = response.as[Suggestions].unsafeRunSync
      suggestions.forall(_.name.input.toLowerCase.contains(headTerm)) shouldBe true
    }
  }

  "GET -> /geo/suggest/{id}/{term}" should {
    "return no suggestions when the locality term has no match" in new IOengineWithDataSuggesters {
      override def dataSuggesterMap: Map[String, DataSuggesters] = _dataSuggesterMap

      val noMatch = "qwerty"
      val responseIO = withSuggestAppRoutes(
        Request[IO](GET, unsafeFromString(s"${C.root}/suggest/${headCountry}/${noMatch}"))
      )
      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

      val suggestions: Suggestions = response.as[Suggestions].unsafeRunSync
      suggestions.size shouldBe 0
    }
  }

  "GET -> /geo/suggest/{id}/{term}" should {
    "return a number of suggestions even for locality terms with Unicode chararacters" in new IOengineWithDataSuggesters {
      override def dataSuggesterMap: Map[String, DataSuggesters] = _dataSuggesterMap

      val responseIO = withSuggestAppRoutes(
        Request[IO](GET, unsafeFromString(s"${C.root}/suggest/${headCountry}/${encode(headTermUnicode, "UTF-8")}"))
      )
      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

      val suggestions: Suggestions = response.as[Suggestions].unsafeRunSync
      suggestions.forall(_.name.input.toLowerCase.contains(headTermUnicode)) shouldBe true
    }
  }

  "GET -> /geo/suggest/{id}/{term}" should {
    "return no more than maxSuggestions, in accordance with the provided parameter" in new IOengineWithDataSuggesters {
      override def dataSuggesterMap: Map[String, DataSuggesters] = _dataSuggesterMap

      val maxSuggestions = 4
      val parameter = s"maxSuggestions=${maxSuggestions}"

      val responseIO = withSuggestAppRoutes(
        Request[IO](GET, unsafeFromString(s"${C.root}/suggest/${tailCountry}/${tailTerm}?${parameter}"))
      )
      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

      val suggestions: Suggestions = response.as[Suggestions].unsafeRunSync
      suggestions.size shouldBe maxSuggestions
    }
  }

  private def cacheUpdate(cache: Cache): IO[Cache] =
    IO.delay {
      val countries = cache.countries.map { country =>
        if (countriesWithSuggestions.contains(country.code)) {
          country.copy(
            localities = (_dataSuggesterMap.get(country.code).map(_.localities.size).getOrElse(1)).toLong,
            updated = now
          )
        } else country
      }
      cache.copy(now, countries, cache.cacheHandlerStopFlag)
    }

  private def withSuggestAppRoutes(request: Request[IO])(implicit E: Engine[IO], S: Sync[IO]): IO[Response[IO]] =
    for {
      implicit0(engineOps: EngineOps[IO]) <- EngineOps[IO](C.clusterName)
      countryCache <- engineOps.init
      updatedCache <- cacheUpdate(countryCache)
      cacheRef <- CacheRef[IO](updatedCache)
      response <- Router((C.root, new SuggestApp[IO](cacheRef, engineOps).routes)).orNotFound.run(request)
    }
    yield response
}
