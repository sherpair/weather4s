package io.sherpair.w4s.auth.app

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.semigroupk._
import io.sherpair.w4s.auth.Auth
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{Member, MemberAction, UpdateRequest}
import io.sherpair.w4s.auth.domain.MemberAction.{MemberDelete, MemberUpdate}
import io.sherpair.w4s.auth.repository.RepositoryMemberOps
import io.sherpair.w4s.domain.{ClaimContent, Logger}
import io.sherpair.w4s.domain.Role.Master
import io.sherpair.w4s.http.{arrayOf, MT}
import org.http4s.{AuthedRequest, AuthedRoutes, EntityDecoder, EntityEncoder, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class MemberApp[F[_]: Sync](
    masterAuth: Auth[F], memberAuth: Auth[F])(
    implicit C: AuthConfig, E: EntityEncoder[F, Member], L: Logger[F], R: RepositoryMemberOps[F]
) extends Http4sDsl[F] {

  implicit val updateRequestDecoder: EntityDecoder[F, UpdateRequest] = jsonOf[F, UpdateRequest]

  val masterOnlyRoutes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case GET -> Root / "member" / LongVar(id) as _ => R.find(id) >>= { memberResponse(_, id) }

      case GET -> Root / "members" as _ => Ok(arrayOf(R.list), MT)
    }

  val memberRoutes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case request @ DELETE -> Root / "member" / LongVar(id) as _ => validateMember(request, id, MemberDelete)

      case request @ PUT -> Root / "member" / LongVar(id) as _ => validateMember(request, id, MemberUpdate)
    }

  val routes = masterAuth(masterOnlyRoutes) <+> memberAuth(memberRoutes)

  private def memberResponse(memberO: Option[Member], id: Long): F[Response[F]] =
    memberO.fold(notFound(id))(Ok(_))

  private def notFound(id: Long): F[Response[F]] = NotFound(s"Member(${id}) is not known")

  private def updateResponse(deletedOrUpdated: Int, id: Long): F[Response[F]] =
    Sync[F].delay(deletedOrUpdated > 0).ifM(NoContent(), notFound(id))

  private def validateMember(
      request: AuthedRequest[F, ClaimContent], id: Long, memberAction: MemberAction
  ): F[Response[F]] =
    if (request.authInfo.role != Master && request.authInfo.id != id) notFound(id)
    else memberAction match {
      case MemberDelete => R.delete(id) >>= { updateResponse(_, id) }

      case MemberUpdate => request.req.decode[UpdateRequest] {
          R.update(id, _) >>= { updateResponse(_, id) }
      }
    }
}
