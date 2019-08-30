package io.sherpair.geo.engine

import scala.annotation.implicitNotFound
import scala.util.{Failure, Success}

import cats.effect.{Async, IO}
import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import io.circe.{Decoder, Encoder, Json, Printer}
import io.circe.jawn.decode

package object elastic {

  private[elastic] implicit class IOLifter[F[_]: Async, T](val io: IO[T]) {
    def lift: F[T] = Async[F].liftIO(io)
  }

  @implicitNotFound("No Decoder found for ${T} type. Import 'io.circe.derivation._' or provide an implicit Decoder instance")
  implicit def implHitReader[T](implicit decoder: Decoder[T]): HitReader[T] =
    (hit: Hit) => decode[T](hit.sourceAsString).fold(Failure(_), Success(_))

  @implicitNotFound("No Encoder found for ${T} type. Import 'io.circe.derivation._' or provide an implicit Encoder instance")
  implicit def implIndexable[T](implicit encoder: Encoder[T], printer: Json => String = printer): Indexable[T] =
    (t: T) => printer(encoder(t))

  private val printer: Json => String = Printer.noSpaces.copy(dropNullValues = true).pretty
}
