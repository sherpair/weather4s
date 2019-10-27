package io.sherpair.w4s.auth.app

import java.nio.charset.StandardCharsets

import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.circe.Decoder
import io.circe.derivation.deriveDecoder
import io.circe.syntax.EncoderOps
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.User
import io.sherpair.w4s.auth.repository.RepositoryUserOps
import io.sherpair.w4s.domain.{Logger, W4sError}
import io.sherpair.w4s.http.{arrayOf, MT}
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Request, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

case class UserBadge(id: String, bytes: Array[Byte], user: Option[User])

object UserBadge {
  implicit val decoder: Decoder[UserBadge] = deriveDecoder[UserBadge]
}

class UserApp[F[_]: Sync](implicit C: AuthConfig, L: Logger[F], R: RepositoryUserOps[F]) extends Http4sDsl[F] {

  implicit val userDecoder: EntityDecoder[F, User] = jsonOf[F, User]
  implicit val userEncoder: EntityEncoder[F, User] = jsonEncoderOf[F, User]

  implicit val userLoginDecoder: EntityDecoder[F, UserBadge] = jsonOf[F, UserBadge]

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "user" / LongVar(key) => R.find(key) >>= { userResponse(_, key.toString) }

    case GET -> Root / "users" => Ok(arrayOf(R.list), MT)

    case request @ POST -> Root / "user" => insertRequest(request)

    case request @ POST -> Root / "user" / "email" => loginRequest("email", request)
    case request @ POST -> Root / "user" / "accountId" => loginRequest("account_id", request)

    case request @ PUT -> Root / "user" / LongVar(key) => updateRequest(request, key)

    case DELETE -> Root / "user" / LongVar(key) =>
      R.delete(key) >>= { updateResponse(_, key.toString) }

    case DELETE -> Root / "user" / "email" / key =>
      R.delete("email", key) >>= { updateResponse(_, key) }

    case DELETE -> Root / "user" / "accountId" / key =>
      R.delete("account_id", key) >>= { updateResponse(_, key) }
  }

  private def insertRequest(request: Request[F]): F[Response[F]] =
    request.decode[UserBadge] { badge =>
      badge.user.fold(BadRequest()) { user =>
        R.insert(user.copy(password = new String(badge.bytes, StandardCharsets.UTF_8)))
          .flatMap(user => Created(user.id.asJson))
          .handleErrorWith {
            case W4sError(msg, _) => Conflict(msg)
          }
      }
    }

  private def loginRequest(fieldId: String, request: Request[F]): F[Response[F]] =
    request.decode[UserBadge] { badge =>
      R.login(fieldId, badge.id, new String(badge.bytes, StandardCharsets.UTF_8)) >>= {
        userResponse(_, badge.id)
      }
    }

  private def updateRequest(request: Request[F], id: Long): F[Response[F]] =
    request.decode[UserBadge] { badge =>
      badge.user.fold(BadRequest()) { user =>
          if (user.id != id) BadRequest()
          else R.update(withPassword(user, badge.bytes))
            .flatMap(updateResponse(_, id.toString))
            .handleErrorWith {
              case W4sError(msg, _) => Conflict(msg)
            }
      }
    }

  private def updateResponse(deletedOrUpdated: Int, key: String): F[Response[F]] =
    Sync[F].delay(deletedOrUpdated > 0).ifM(NoContent(), NotFound(s"User(${key}) is not known"))

  private def userResponse(user: Option[User], key: String): F[Response[F]] =
    user.fold(NotFound(s"User(${key}) is not known"))(Ok(_))

  private def withPassword(user: User, bytes: Array[Byte]): User =
    if (bytes.length > 0) user.copy(password = new String(bytes, StandardCharsets.UTF_8))
    else user
}
