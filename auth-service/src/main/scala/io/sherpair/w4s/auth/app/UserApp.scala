package io.sherpair.w4s.auth.app

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.semigroupk._
import io.sherpair.w4s.auth.{Authoriser, Claims}
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{UpdateRequest, User, UserAction}
import io.sherpair.w4s.auth.domain.UserAction.{UserDelete, UserUpdate}
import io.sherpair.w4s.auth.repository.RepositoryUserOps
import io.sherpair.w4s.domain.{AuthData, ClaimContent, Logger}
import io.sherpair.w4s.domain.Role.Master
import io.sherpair.w4s.http.{arrayOf, MT}
import org.http4s.{AuthedRequest, AuthedRoutes, EntityDecoder, EntityEncoder, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class UserApp[F[_]: Sync](
    authData: AuthData)(implicit C: AuthConfig, E: EntityEncoder[F, User], L: Logger[F], R: RepositoryUserOps[F]
) extends Http4sDsl[F] {

  implicit val updateRequestDecoder: EntityDecoder[F, UpdateRequest] = jsonOf[F, UpdateRequest]

  val masterOnlyRoutes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case GET -> Root / "user" / LongVar(id) as _ => R.find(id) >>= { userResponse(_, id) }

      case GET -> Root / "users" as _ => Ok(arrayOf(R.list), MT)
    }

  val memberRoutes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case request @ DELETE -> Root / "user" / LongVar(id) as _ => validateUser(request, id, UserDelete)

      case request @ PUT -> Root / "user" / LongVar(id) as _ => validateUser(request, id, UserUpdate)
    }

  val masterAuthoriser = Authoriser(authData, Claims.audAuth, _.role == Master)
  val memberAuthoriser = Authoriser(authData, Claims.audAuth)

  val routes = masterAuthoriser(masterOnlyRoutes) <+> memberAuthoriser(memberRoutes)

  private def notFound(id: Long): F[Response[F]] = NotFound(s"User(${id}) is not known")

  private def userResponse(userO: Option[User], id: Long): F[Response[F]] =
    userO.fold(notFound(id))(Ok(_))

  private def updateResponse(deletedOrUpdated: Int, id: Long): F[Response[F]] =
    Sync[F].delay(deletedOrUpdated > 0).ifM(NoContent(), notFound(id))

  private def validateUser(request: AuthedRequest[F, ClaimContent], id: Long, userAction: UserAction): F[Response[F]] =
    if (request.authInfo.role != Master && request.authInfo.id != id) notFound(id)
    else userAction match {
      case UserDelete => R.delete(id) >>= { updateResponse(_, id) }

      case UserUpdate => request.req.decode[UpdateRequest] {
          R.update(id, _) >>= { updateResponse(_, id) }
      }
    }
}
