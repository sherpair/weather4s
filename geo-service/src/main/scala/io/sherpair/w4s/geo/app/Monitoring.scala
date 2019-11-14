package io.sherpair.w4s.geo.app

import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.syntax._
import io.sherpair.w4s.auth.Auth
import io.sherpair.w4s.config.Config4e
import io.sherpair.w4s.domain.ClaimContent
import io.sherpair.w4s.engine.Engine
import org.http4s.{AuthedRoutes, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class Monitoring[F[_]: Sync](auth: Auth[F])(implicit C: Config4e, E: Engine[F]) extends Http4sDsl[F] {

  val masterOnlyRoutes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case GET -> Root / "health" as _ => healthCheck
    }

  val routes = auth(masterOnlyRoutes)

  private def healthCheck: F[Response[F]] =
    for {
      (attempts, status) <-
        E.healthCheck.handleErrorWith {
          error => Sync[F].delay((C.healthAttemptsES, error.getMessage))
        }

      response <- Ok(Map("attempts" -> attempts.toString, "engine" -> status).asJson)
    }
    yield response
}
