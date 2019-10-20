package io.sherpair.w4s.engine.memory

import java.io.{ByteArrayInputStream, ObjectInputStream}

import scala.jdk.CollectionConverters._

import cats.effect.Concurrent
import cats.effect.concurrent.Ref
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import io.sherpair.w4s.config.{Suggestions => Parameters}
import io.sherpair.w4s.domain.{
  noBulkErrors, BulkErrors, Country, Localities, Locality, Suggestion, Suggestions
}
import io.sherpair.w4s.engine.LocalityIndex

object MemoryLocalityIndex {

  def apply[F[_]](dataSuggesterMap: Map[String, DataSuggesters])(implicit Cn: Concurrent[F]): F[LocalityIndex[F]] =
    Ref.of[F, Map[String, DataSuggesters]](dataSuggesterMap).map { ref =>
      new LocalityIndex[F] {

        override def count(country: Country): F[Long] =
          ref.get.map(_.get(country.code).map(_.localities.size.toLong).getOrElse(-1))

        override def getById(country: Country, geoId: String): F[Option[Locality]] =
          ref.get.map(_.get(country.code).flatMap(_.localities.get(geoId)))

        override def delete(country: Country): F[Unit] = ref.update(_ - country.code)

        override def saveAll(country: Country, localities: Localities): F[BulkErrors] =
          ref.get.flatMap(
            _.get(country.code)
              .fold(Cn.raiseError[BulkErrors](new RuntimeException(s"${country} not in DataSuggesters map"))) {
                _ => Cn.delay(noBulkErrors)
              }
          )

        override def suggest(country: Country, localityTerm: String, p: Parameters): F[Suggestions] =
          ref.get.flatMap(
            _.get(country.code)
              .fold(Cn.raiseError[Suggestions](new RuntimeException(s"${country} not in DataSuggesters map"))) {
                lookup(_, localityTerm, p.maxSuggestions)
              }
          )

        override def suggestByAsciiOnly(country: Country, localityTerm: String, p: Parameters): F[Suggestions] =
          suggest(country, localityTerm, p)

        private def deserialise(bytes: Array[Byte]): Suggestion = {
          val ois = new ObjectInputStream(new ByteArrayInputStream(bytes))
          val suggestion = ois.readObject.asInstanceOf[Suggestion]
          ois.close()
          suggestion
        }

        private def lookup(dataSuggesters: DataSuggesters, localityTerm: String, maxSuggestions: Int): F[Suggestions] = {
          val iterator = dataSuggesters.suggesters.iterator
          Cn.tailRecM[OSuggester, Suggestions](iterator.next) { suggesterO =>
            suggesterO.fold(List.empty[Suggestion].asRight[OSuggester].pure[F]) { suggester =>
              val result = Stream.emits(
                suggester.lookup(localityTerm, false, maxSuggestions).asScala
              )
              .map(lookupResult => deserialise(lookupResult.payload.bytes)).toList

              Cn.delay(if (result.isEmpty) iterator.next.asLeft[Suggestions] else result.asRight[OSuggester])
            }
          }
        }
      }
    }
}
