package io.sherpair.w4s.geo.http

import scala.concurrent.ExecutionContext.global

import cats.effect.ConcurrentEffect
import cats.syntax.apply._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.w4s.config.Http.host
import io.sherpair.w4s.domain.Country
import io.sherpair.w4s.geo.config.Configuration
import org.http4s.{HttpVersion, ParseFailure, Request, Response, Uri}
import org.http4s.Method.PUT
import org.http4s.Status.InternalServerError
import org.http4s.client.blaze.BlazeClientBuilder

object Loader {

  def apply[F[_] : Logger](country: Country)(implicit C: Configuration, CE: ConcurrentEffect[F]): F[Response[F]] = {
    val uri = s"http://${host(C.httpLoader)}/country/${country.code}"
    Uri.fromString(uri).fold(logUriError(_), sendRequest(_))
  }

  private def sendRequest[F[_]](uri: Uri)(implicit CE: ConcurrentEffect[F]): F[Response[F]] =
    BlazeClientBuilder[F](global).resource.use {
      _.fetch(Request[F](PUT, uri, HttpVersion.`HTTP/2.0`))(CE.delay(_))
    }

  private def logUriError[F[_] : Logger](failure: ParseFailure)(implicit CE: ConcurrentEffect[F]): F[Response[F]] =
    Logger[F].error(failure)("Geo.Loader: Bug or Missing Configuration?") *> CE.delay(Response[F](InternalServerError))
}
