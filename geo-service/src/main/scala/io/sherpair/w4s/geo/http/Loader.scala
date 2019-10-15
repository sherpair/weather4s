package io.sherpair.w4s.geo.http

import cats.effect.ConcurrentEffect
import cats.syntax.apply._
import io.circe.syntax._
import io.sherpair.w4s.domain.{Country, Logger}
import io.sherpair.w4s.geo.config.GeoConfig
import org.http4s.{EntityBody, ParseFailure, Request, Response, Uri}
import org.http4s.Method.PUT
import org.http4s.Status.InternalServerError
import org.http4s.circe._
import org.http4s.client.Client

class Loader[F[_]](client: Client[F], country: Country, host: String)(implicit CE: ConcurrentEffect[F], L: Logger[F]) {

  private val send: F[Response[F]] = {
    val uri = s"http://${host}/loader/country/${country.code}"
    Uri.fromString(uri).fold(logUriError(_), sendRequest(_))
  }

  private def sendRequest(uri: Uri): F[Response[F]] =
    L.info(s"""Sending request "Loading ${country}" to ${host}""") *>
      // Idempotent PUT.
      client.fetch(Request[F](PUT, uri).withEntity(country.asJson))(CE.delay(_))

  private def logUriError(failure: ParseFailure): F[Response[F]] =
    L.error(failure)("Geo.Loader: Bug or Missing Configuration?") *>
      CE.delay(Response[F](InternalServerError))
}

object Loader {

  def apply[F[_]: ConcurrentEffect](
      client: Client[F], country: Country, body: EntityBody[F])(implicit C: GeoConfig, L: Logger[F]
  ): F[Response[F]] =
    new Loader[F](client, country, C.hostLoader.joined) send
}
