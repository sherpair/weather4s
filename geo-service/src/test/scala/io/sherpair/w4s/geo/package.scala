package io.sherpair.w4s

import scala.concurrent.ExecutionContext.global

import cats.effect.{Blocker, ContextShift, IO, Resource, Timer}
import io.chrisdavenport.log4cats.noop.NoOpLogger
import io.circe.Decoder
import io.circe.derivation.deriveDecoder
import io.sherpair.w4s.domain.{noBulkErrors, BulkErrors, Country, Logger, Suggestion}
import io.sherpair.w4s.domain.Analyzer.indonesian
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.engine.memory.{DataSuggesters, MemoryEngine, MemoryLoader}
import io.sherpair.w4s.geo.config.GeoConfig
import io.sherpair.w4s.types.Suggestions
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import org.scalatest.{EitherValues, Matchers, OptionValues, PrivateMethodTester, WordSpec}

package object geo {

  trait GeoSpec
    extends WordSpec
      with Matchers
      with EitherValues
      with OptionValues
      with PrivateMethodTester {

    val countryUnderTest = Country("id", "Indonesia", indonesian)

    implicit val C: GeoConfig = GeoConfig()
    implicit val timer: Timer[IO] = IO.timer(global)
    implicit val logger: Logger[IO] = NoOpLogger.impl[IO]
  }

  trait DataSuggesterMap {

    implicit val suggestionDecoder: Decoder[Suggestion] = deriveDecoder[Suggestion]
    implicit val suggestionsDecoder: EntityDecoder[IO, Suggestions] = jsonOf

    val countriesWithSuggestions = List("lu", "sg")  // Luxembourg and Singapore

    val headCountry = countriesWithSuggestions.head
    val tailCountry = countriesWithSuggestions.tail.head

    val headTerm = "sch"
    val tailTerm = "ter"

    // scalastyle:off
    val headTermUnicode = "m\u00fcn"  // "mün"
    // scalastyle:on

    val _dataSuggesterMap: Map[String, DataSuggesters] =
      Blocker[IO]
        .flatMap(blocker => Resource.liftF(IO(MemoryLoader(countriesWithSuggestions, blocker))))
        .use(IO(_))
        .unsafeRunSync
  }

  trait IOimplicits {
    implicit val resultForSaveAll: BulkErrors = noBulkErrors
    implicit val cs: ContextShift[IO] = IO.contextShift(global)
  }

  trait IOengine extends IOimplicits {
    implicit val engine: Engine[IO] =
      MemoryEngine[IO](Map.empty[String, DataSuggesters]).unsafeRunSync
  }

  trait IOengineWithDataSuggesters extends IOimplicits {

    def dataSuggesterMap: Map[String, DataSuggesters]

    implicit val engine: Engine[IO] =
      MemoryEngine[IO](dataSuggesterMap).unsafeRunSync
  }
}
