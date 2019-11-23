package io.sherpair.w4s.geo.http

import cats.effect.ConcurrentEffect
import cats.syntax.apply._
import io.circe.syntax._
import io.sherpair.w4s.auth.addBearerTokenToRequest
import io.sherpair.w4s.domain.{Country, Logger}
import io.sherpair.w4s.geo.config.GeoConfig
import org.http4s.{ParseFailure, Request, Response, Uri}
import org.http4s.Method.PUT
import org.http4s.Status.InternalServerError
import org.http4s.circe._
import org.http4s.client.Client

class Loader[F[_]](
    client: Client[F], country: Country, loaderUrl: String, token: String)(
    implicit CE: ConcurrentEffect[F], L: Logger[F]
) {

  private val send: F[Response[F]] = {
    val uri = s"${loaderUrl}/${country.code}"
    Uri.fromString(uri).fold(logUriError(_), sendRequest(_))
  }

  private def sendRequest(uri: Uri): F[Response[F]] =
    L.info(s"""Sending request "Loading ${country}" to ${loaderUrl}""") *>
      // Idempotent PUT.
      client.fetch(
        addBearerTokenToRequest(Request[F](PUT, uri).withEntity(country.asJson).withHeaders(), token)
      )(CE.delay(_))

  private def logUriError(failure: ParseFailure): F[Response[F]] =
    L.error(failure)("Geo.Loader: Bug or Missing Configuration while composing the Uri?") *>
      CE.delay(Response[F](InternalServerError))
}

object Loader {

  def apply[F[_]: ConcurrentEffect](
      client: Client[F], country: Country, loaderUrl: String, token: String)(implicit C: GeoConfig, L: Logger[F]
  ): F[Response[F]] =
    new Loader[F](client, country, loaderUrl, token) send
}
