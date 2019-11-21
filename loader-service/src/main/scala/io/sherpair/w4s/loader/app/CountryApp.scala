package io.sherpair.w4s.loader.app

import cats.effect.{Fiber, Sync}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.option._
import cats.syntax.semigroupk._
import fs2.concurrent.NoneTerminatedQueue
import io.sherpair.w4s.auth.masterOnly
import io.sherpair.w4s.domain.{ClaimContent, Country, Logger}
import io.sherpair.w4s.loader.config.LoaderConfig
import org.http4s.{AuthedRoutes, EntityDecoder}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class CountryApp[F[_]: Sync](
    queue: NoneTerminatedQueue[F, Country], loaderFiber: Fiber[F, Unit])(
    implicit C: LoaderConfig, L: Logger[F]
) extends Http4sDsl[F] {

  implicit val countryEncoder: EntityDecoder[F, Country] = jsonOf[F, Country]

  private val masterOnlyRoutes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case GET -> Root / "quit" as cC =>
        masterOnly(cC,
          queue.enqueue1(None) >> loaderFiber.join >> L.warn("Country Loader stopped!!") *> NoContent()
        )
    }

  private val memberRoutes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case request @ PUT -> Root / "country" / id as _ =>
        (request.req.as[Country] >>= { country =>
          queue.enqueue1(country.copy(code = country.code.toLowerCase).some)
        }) *> NoContent()
  }

  val routes = masterOnlyRoutes <+> memberRoutes
}
