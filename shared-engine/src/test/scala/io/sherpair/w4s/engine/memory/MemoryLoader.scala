package io.sherpair.w4s.engine.memory

import java.io.{ByteArrayOutputStream, InputStream, ObjectOutputStream}
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentLinkedQueue

import scala.concurrent.ExecutionContext.global
import scala.jdk.CollectionConverters._

import cats.effect.{Blocker, ContextShift, IO, Resource, Sync}
import cats.syntax.option._
import fs2.text
import io.sherpair.w4s.domain.{Locality, Name, Suggestion}
import io.sherpair.w4s.types.Localities
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.search.suggest.InputIterator
import org.apache.lucene.search.suggest.analyzing.{
  AnalyzingInfixSuggester, AnalyzingSuggester, FuzzySuggester
}
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.lucene.util.BytesRef

object MemoryLoader {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  def apply(countries: List[String], blocker: Blocker): Map[String, DataSuggesters] =
    countries
      .map { country =>
        Resource
          .fromAutoCloseable(IO(getClass.getResourceAsStream(s"/${country.toLowerCase}.txt")))
          .use(createDataSuggesters[IO](_, country, blocker))
      }
      .map(_.unsafeRunSync)
      .foldLeft(Map[String, DataSuggesters]()) { (map, data) => map + data }

  // scalastyle:off magic.number
  private def createDataSuggesters[F[_]: ContextShift: Sync](
      is: InputStream, country: String, blocker: Blocker
  ): F[(String, DataSuggesters)] =
    fs2.io.readInputStream(Sync[F].delay(is), 4096, blocker)
      .through(text.utf8Decode)
      .through(text.lines)
      .filter(_.chars.filter(_ == '\t').count > 8)
      .fold[Localities](List.empty[Locality]) { (list, line) =>
        list.appended(Locality(line.split("\t")))
      }
      .map { localities =>
        (country.toLowerCase -> DataSuggesters(
          createSuggesters(localities),
          localities.foldLeft(Map.empty[String, Locality]) {
            (map, locality) => map + (locality.geoId -> locality)
          }
        ))
      }.compile.lastOrError
  // scalastyle:on magic.number

  private def createSuggesters(localities: Localities): Suggesters = {
    // Could a single StandardAnalyzer instance be used for all Suggesters ?
    val baseSuggester = new AnalyzingSuggester(new ByteBuffersDirectory, "", new StandardAnalyzer())
    val fuzzySuggester = new FuzzySuggester(new ByteBuffersDirectory, "", new StandardAnalyzer())
    val infixSuggester = new AnalyzingInfixSuggester(new ByteBuffersDirectory, new StandardAnalyzer())

    baseSuggester.build(new LocalityIterator(new ConcurrentLinkedQueue[Locality](localities.asJava)))
    fuzzySuggester.build(new LocalityIterator(new ConcurrentLinkedQueue[Locality](localities.asJava)))
    infixSuggester.build(new LocalityIterator(new ConcurrentLinkedQueue[Locality](localities.asJava)))

    List(baseSuggester.some, fuzzySuggester.some, infixSuggester.some, none)
  }
}

class LocalityIterator(localities: ConcurrentLinkedQueue[Locality]) extends InputIterator {

  // scalastyle:off
  var current: Option[Locality] = None
  // scalastyle:on

  /*
   Order of calls from Lucene at every iteration is: next() then payload() then weight().
   Following that I could call peek() on next() and payload() and poll() on weight(),
   but I don't want to rely on that. That's the reason for using a var.
   */
  override def contexts: java.util.Set[BytesRef] = Set.empty[BytesRef].asJava
  override def hasContexts: Boolean = false
  override def hasPayloads: Boolean = true
  override def next: BytesRef = {
    current = Option(localities.poll)
    current.map(l => new BytesRef(l.asciiOnly.getBytes(Charset.defaultCharset))).orNull
  }

  override def payload: BytesRef = current.map(serialize(_)).orNull
  override def weight: Long = current.map(_.population.toLong).getOrElse(0L)

  private def serialize(loc: Locality): BytesRef = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(Suggestion(Name(loc.name, loc.population), loc.coord, loc.tz))
    oos.close
    new BytesRef(baos.toByteArray)
  }
}
