package io.sherpair.w4s.loader.app

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.file.{Files, Path, StandardCopyOption}
import java.util.zip.ZipInputStream

import cats.effect.{Blocker, ContextShift, Sync}
import cats.instances.int._
import cats.instances.tuple._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.flatMap._
import fs2.text
import io.sherpair.w4s.domain.{BulkError, Country, Locality, Logger, W4sError}
import io.sherpair.w4s.loader.config.LoaderConfig
import io.sherpair.w4s.loader.domain.LoaderAccums
import io.sherpair.w4s.loader.engine.EngineOps
import org.http4s.EntityDecoder
import org.http4s.client.Client

class LoaderRun[F[_]: ContextShift](
    client: Client[F], country: Country)(implicit C: LoaderConfig, eOps: EngineOps[F], L: Logger[F], S: Sync[F]) {

  val countryCodeUpperCase = country.code.toUpperCase

  def start: F[Unit] =
    (download(s"${C.countryDownloadUrl}/${countryCodeUpperCase}.zip") >>= { path =>
      L.info(s"Streaming ${path.getFileName} to Engine...")
      S.bracket(
        S.delay(new ZipInputStream(Files.newInputStream(path)))
      )(
        _.tailRecM[F, ZipInputStream](locateZipEntry(_)) >>= {
          eOps.prepareEngineFor(country) >> streamToEngine(_) >>= { lA =>
            eOps.updateEngineFor(country, lA.getOrElse(LoaderAccums(0L, 0)))
          }
        }
      )(
        zis => S.delay(zis.closeEntry()) >> S.delay(zis.close)
      )
    })
    .handleErrorWith(logDownloadError(_))

  private def chunkToEngine(country: Country, localities: List[Locality]): F[(Int, List[BulkError])] =
    eOps.saveAllLocalities(country, localities) >>= {errors => (localities.size, errors).pure[F]}

  private def download(uri: String): F[Path] =
    L.info(s"""Download of "${uri}" begins...""") *> S.bracket(
      client.expect[InputStream](uri))(is => toTempFile(is))(is => S.delay(is.close())
    )

  type ZE = Either[ZipInputStream, ZipInputStream]

  val countryFile = s"${countryCodeUpperCase}.txt"

  private def locateZipEntry(zis: ZipInputStream): F[ZE] =
    Option(zis.getNextEntry).fold {
      S.raiseError[ZE](W4sError(s"""No zip entry named "${countryFile}" in (${countryCodeUpperCase}.zip"""))
    } { ze =>
      if (ze.getName == countryFile) S.delay(zis.asRight)
      else S.delay(zis.closeEntry()) >> S.delay(zis.asLeft[ZipInputStream])
    }

  private def logDownloadError(error: Throwable): F[Unit] =
    L.error(error)(s"While downloading (${countryCodeUpperCase}.zip)")

  val bufferSize = 8192
  val chunkSize = 1000
  val minFieldsPerLine = 7

  private def streamToEngine(is: InputStream): F[Option[LoaderAccums]] =
    Blocker[F].use {
      fs2.io.readInputStream(is.pure[F], bufferSize, _, false)
        .through(text.utf8Decode)
        .through(text.lines)
        .filter(_.chars.filter(_ == '\t').count > minFieldsPerLine)
        .map(line => Locality(line.split("\t")))
        .chunkN(chunkSize, true)
        .evalMap(chunk => chunkToEngine(country, chunk.toList))
        .foldMap[(Int, Int)](accums => (accums._1, accums._2.size)) // count localities and bulk errors
        .evalMap[F, LoaderAccums] { accums =>
          S.delay(LoaderAccums(localities = accums._1.toLong, bulkErrors = accums._2))
        }
        .compile.last
    }

  private def toTempFile(is: InputStream): F[Path] = {
    val tempFile = Files.createTempFile(s"GEO-${countryCodeUpperCase}-", ".tmp.zip")
    val bytesRead = Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING)
    L.info(s"Saved ${bytesRead} bytes to ${tempFile}")
    S.delay(tempFile)
  }

  implicit def decoder: EntityDecoder[F, InputStream] =
    EntityDecoder.byteArrayDecoder.map(new ByteArrayInputStream(_))
}

object LoaderRun {

  def apply[F[_]: ContextShift](
      client: Client[F], country: Country)(implicit C: LoaderConfig, E: EngineOps[F], L: Logger[F], S: Sync[F]
  ): F[Unit] =
    new LoaderRun[F](client, country).start
}
