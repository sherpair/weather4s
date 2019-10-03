package io.sherpair.w4s.loader.app

import cats.effect.{Fiber, Sync}
import cats.syntax.apply._
import cats.syntax.flatMap._
import fs2.concurrent.NoneTerminatedQueue
import io.sherpair.w4s.domain.{Country, Logger}
import io.sherpair.w4s.loader.config.LoaderConfig
import org.http4s.{EntityDecoder, HttpRoutes}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class CountryApp[F[_]: Sync](queue: NoneTerminatedQueue[F, Country], loaderFiber: Fiber[F, Unit])(
    implicit C: LoaderConfig, L: Logger[F]
) extends Http4sDsl[F] {

  implicit val countryEncoder: EntityDecoder[F, Country] = jsonOf[F, Country]

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case request@PUT -> Root / "country" / id =>
      (request.as[Country] >>= { country =>
        queue.enqueue1(Some(country.copy(code = country.code.toLowerCase)))
      }) *> Ok()

    case GET -> Root / "quit" =>
      queue.enqueue1(None) >> loaderFiber.join >> L.warn("Country Loader stopped!!") *> Ok()
  }
}
