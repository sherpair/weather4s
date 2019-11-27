package io.sherpair.w4s.http

import java.nio.charset.StandardCharsets.UTF_8

import cats.effect.{Blocker, ContextShift => CS, Sync}
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import io.circe.Json
import io.sherpair.w4s.config.Configuration
import io.sherpair.w4s.domain.{loadResource, Logger}
import org.http4s.{HttpRoutes, Request, Response, StaticFile}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.syntax.literals._
import org.webjars.WebJarAssetLocator

class ApiApp[F[_]: CS: Sync](implicit blocker: Blocker, C: Configuration, L: Logger[F]) extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case       GET -> Root / "api" => PermanentRedirect(Location(uri"api/index.html"))

    case req @ GET -> Root / "api" / "index.html" => sendResource("/api/index.html", req)

    case       GET -> Root / "api" / "config.json" =>
      Ok(Json.obj("url" -> Json.fromString(s"${C.root}/api/openapi.yml")))

    case       GET -> Root / "api" / "openapi.yml" => openAPISpec >>= { Ok(_, YamlMT) }

    case req @ GET -> Root / "api" / file =>
      swaggerResource(file) >>= { _.fold(NotFound(file))(sendResource(_, req)) }
  }

  private lazy val openAPISpec: F[String] =
    L.info("Loading OpenAPI specification(openapi.yml)") *>
      loadResource("/openapi.yml").map {
        new String(_, UTF_8).replaceFirst("==server-address==", serverRoot)
      }

  private def sendResource(name: String, request: Request[F]): F[Response[F]] =
    StaticFile.fromResource(name, blocker, request.some).getOrElseF(NotFound())

  private def swaggerResource(file: String): F[Option[String]] =
    Option(new WebJarAssetLocator().getWebJars.get("swagger-ui")).fold {
      L.error(s"Version of swagger-ui webjar cannot be determined for file(${file})") *> none[String].pure[F]
    } {
      version => s"/META-INF/resources/webjars/swagger-ui/${version}/${file}".some.pure[F]
    }
}
