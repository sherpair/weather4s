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
import io.sherpair.w4s.config.Service
import io.sherpair.w4s.domain.{BulkError, GeoPoint, Location, W4sError}
import org.apache.lucene.document.{Document, LatLonDocValuesField, LatLonPoint, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.search.SortField.Type

class MemoryEngineLocation[F[_]: Sync](ref: Ref[F, SearchableLocation])(implicit S: Service) {

  def getById(id: String): F[Option[Location]] = Sync[F].delay(None)

  def loadAll(sortBy: Option[Seq[String]], windowSize: Int): F[List[Location]] = Sync[F].delay(List.empty)

  def saveAll(list: List[Location]): F[List[BulkError]] = Sync[F].delay(List.empty)

  // This method should be never called, as countries are never updated.
  // Only used to insert the Meta record.
  def upsert(location: Location): F[String] =
    Sync[F].raiseError[String](W4sError(s"Bug in LocationEngineIndex: upsert should never be called"))
}

trait SearchableLocation extends Searchable[Location] {
  def geoId: Field[Int]
  override def idSearchTerms(location: Location): List[SearchTerm] = List(exact(geoId(location.geoId)))
}

object MemoryEngineLocation {
  def apply[F[_]: Sync](implicit S: Service): F[MemoryEngineLocation[F]] = {
    val lucene = new DirectLucene(uniqueFields = List("geoId"))
    Ref.of[F, SearchableLocation](lucene.create.searchable[SearchableLocation]).map(new MemoryEngineLocation[F](_))
  }

  implicit val geoValueSupport: ValueSupport[GeoPoint] = GeoPointValueSupport

  object GeoPointValueSupport extends ValueSupport[GeoPoint] {
    override def store(field: Field[GeoPoint], value: GeoPoint, document: Document): Unit = {
      val stored = new StoredField(field.storeName, value.toString)
      document.add(stored)
    }

    override def filter(field: Field[GeoPoint], value: GeoPoint, document: Document): Unit = {
      val filtered = new LatLonPoint(field.filterName, value.latitude, value.longitude)
      document.add(filtered)
    }

    override def sorted(field: Field[GeoPoint], value: GeoPoint, document: Document): Unit = {
      val sorted = new LatLonDocValuesField(field.sortName, value.latitude, value.longitude)
      document.add(sorted)
    }

    override def fromLucene(fields: List[IndexableField]): GeoPoint = fromString(fields.head.stringValue())

    override def sortFieldType: Type = SortField.Type.SCORE

    // Ignored... GeoPoint is not used as a SearchTerm.
    override def searchTerm(fv: FieldAndValue[GeoPoint]): SearchTerm = MatchAllSearchTerm

    def fromString(s: String): GeoPoint = {
      val index = s.indexOf('x')
      val latitude = s.substring(0, index).toDouble
      val longitude = s.substring(index + 1).toDouble
      GeoPoint(latitude, longitude)
    }
  }
}
