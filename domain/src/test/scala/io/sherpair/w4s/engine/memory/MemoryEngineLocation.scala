package io.sherpair.w4s.engine.memory

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.functor._
import com.outr.lucene4s.{exact, DirectLucene}
import com.outr.lucene4s.field.Field
import com.outr.lucene4s.field.value.FieldAndValue
import com.outr.lucene4s.field.value.support.ValueSupport
import com.outr.lucene4s.mapper.Searchable
import com.outr.lucene4s.query.{MatchAllSearchTerm, SearchTerm}
import io.sherpair.w4s.config.Configuration
import io.sherpair.w4s.domain.{BulkError, GeoPoint, Locality, W4sError}
import org.apache.lucene.document.{Document, LatLonDocValuesField, LatLonPoint, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

class MemoryEngineLocality[F[_]: Sync](ref: Ref[F, SearchableLocality])(implicit C: Configuration) {

  def getById(id: String): F[Option[Locality]] = Sync[F].delay(None)

  def loadAll(sortBy: Option[Seq[String]], windowSize: Int): F[List[Locality]] = Sync[F].delay(List.empty)

  def saveAll(list: List[Locality]): F[List[BulkError]] = Sync[F].delay(List.empty)

  // This method should be never called, as countries are never updated.
  // Only used to insert the Meta record.
  def upsert(locality: Locality): F[String] =
    Sync[F].raiseError[String](W4sError(s"Bug in LocalityEngineIndex: upsert should never be called"))
}

trait SearchableLocality extends Searchable[Locality] {
  def geoId: Field[String]
  override def idSearchTerms(locality: Locality): List[SearchTerm] = List(exact(geoId(locality.geoId)))
}

object MemoryEngineLocality {
  def apply[F[_]: Sync](implicit C: Configuration): F[MemoryEngineLocality[F]] = {
    val lucene = new DirectLucene(uniqueFields = List("geoId"))
    Ref.of[F, SearchableLocality](lucene.create.searchable[SearchableLocality]).map(new MemoryEngineLocality[F](_))
  }

  implicit val geoValueSupport: ValueSupport[Option[GeoPoint]] = GeoPointValueSupport

  object GeoPointValueSupport extends ValueSupport[Option[GeoPoint]] {
    override def store(field: Field[Option[GeoPoint]], value: Option[GeoPoint], document: Document): Unit = {
      val stored = new StoredField(field.storeName, value.toString)
      document.add(stored)
    }

    override def filter(field: Field[Option[GeoPoint]], value: Option[GeoPoint], document: Document): Unit = {
      val filtered = value.fold(
        new LatLonPoint(field.filterName, 0, 0)
      )(gp => new LatLonPoint(field.filterName, gp.lat, gp.lon))
      document.add(filtered)
    }

    override def sorted(field: Field[Option[GeoPoint]], value: Option[GeoPoint], document: Document): Unit = {
      val sorted = value.fold(
        new LatLonDocValuesField(field.sortName, 0, 0)
      )(gp => new LatLonDocValuesField(field.sortName, gp.lat, gp.lon))
      document.add(sorted)
    }

    override def fromLucene(fields: List[IndexableField]): Option[GeoPoint] = fromString(fields.head.stringValue())

    override def sortFieldType: Type = SortField.Type.SCORE

    // Ignored... GeoPoint is not used as a SearchTerm.
    override def searchTerm(fv: FieldAndValue[Option[GeoPoint]]): SearchTerm = MatchAllSearchTerm

    def fromString(s: String): Option[GeoPoint] = {
      val index = s.indexOf('x')
      val latitude = s.substring(0, index).toDouble
      val longitude = s.substring(index + 1).toDouble
      Some(GeoPoint(latitude, longitude))
    }
  }
}
