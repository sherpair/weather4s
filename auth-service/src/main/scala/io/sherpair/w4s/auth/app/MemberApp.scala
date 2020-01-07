package io.sherpair.w4s.auth.app

import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.semigroupk._
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{Member, MemberAction, MemberRequest, UniqueViolation, UpdateRequest}
import io.sherpair.w4s.auth.domain.EmailType.Activation
import io.sherpair.w4s.auth.domain.MemberAction.{ChangeEmail, ChangeSecret, MemberDelete, MemberUpdate}
import io.sherpair.w4s.auth.masterOnly
import io.sherpair.w4s.auth.repository.RepositoryMemberOps
import io.sherpair.w4s.domain.{ClaimContent, Logger}
import io.sherpair.w4s.domain.Role.Master
import io.sherpair.w4s.http.{arrayOf, JsonMT}
import org.http4s.{AuthedRequest, AuthedRoutes, EntityDecoder, EntityEncoder, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class MemberApp[F[_]](
    auth: Authenticator[F], tokenOps: TokenOps[F])(
    implicit C: AuthConfig, E: EntityEncoder[F, Member], L: Logger[F], R: RepositoryMemberOps[F], S: Sync[F]
) extends Http4sDsl[F] {

  implicit val secretDecoder: EntityDecoder[F, MemberRequest] = jsonOf
  implicit val updateRequestDecoder: EntityDecoder[F, UpdateRequest] = jsonOf[F, UpdateRequest]

  private val masterOnlyRoutes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case GET -> Root / "member" / LongVar(id) as cC => masterOnly(cC, R.find(id) >>= { memberResponse(_, id) })

      case GET -> Root / "members" as cC => masterOnly(cC, Ok(arrayOf(R.list), JsonMT))
    }

  private val memberRoutes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case request @ DELETE -> Root / "member" / LongVar(id) as _ => validateMember(request, id, MemberDelete)

      case request @ POST -> Root / "email" / LongVar(id) as _ => validateMember(request, id, ChangeEmail)

      case request @ POST -> Root / "secret" / LongVar(id) as _ => validateMember(request, id, ChangeSecret)

      case request @ PUT -> Root / "member" / LongVar(id) as _ => validateMember(request, id, MemberUpdate)
    }

  val routes = masterOnlyRoutes <+> memberRoutes

  private def emailUpdate(request: AuthedRequest[F, ClaimContent], id: Long): F[Response[F]] =
    request.req.as[MemberRequest] >>= { memberRequest =>
      val result = R.update(id, memberRequest.accountId) >>= {
        _.fold(notFoundResponse(id))(tokenOps.send(_, Activation) *> NoContent())
      }

      result.recoverWith {
        case UniqueViolation(msg) => Conflict(msg)
      }
    }

  private def memberResponse(memberO: Option[Member], id: Long): F[Response[F]] =
    memberO.fold(notFoundResponse(id))(Ok(_))

  private def memberUpdate(request: AuthedRequest[F, ClaimContent], id: Long): F[Response[F]] =
    request.req.as[UpdateRequest] >>= { updateRequest =>
      val result = R.update(id, updateRequest) >>= {
        _.fold(notFoundResponse(id))(auth.addJwtToAuthorizationHeader(NoContent(), _))
      }

      result.recoverWith {
        case UniqueViolation(msg) => Conflict(msg)
      }
    }

  private def noContentResponse(deletedOrUpdated: Int, id: Long): F[Response[F]] =
    Sync[F].delay(deletedOrUpdated > 0).ifM(NoContent(), notFoundResponse(id))

  private def notFoundResponse(id: Long): F[Response[F]] = NotFound(s"Member(${id}) is not known")

  private def secretUpdate(request: AuthedRequest[F, ClaimContent], id: Long): F[Response[F]] =
    request.req.decode[MemberRequest] { memberRequest =>
      S.delay(memberRequest.hasLegalSecret).ifM(
        R.update(id, memberRequest.secret) >>= { noContentResponse(_, id) },
        NotAcceptable(memberRequest.illegalSecret)
      )
    }

  private def validateMember(
      request: AuthedRequest[F, ClaimContent], id: Long, memberAction: MemberAction
  ): F[Response[F]] =
    if (request.context.role != Master && request.context.id != id) notFoundResponse(id)
    else memberAction match {
      case ChangeEmail => emailUpdate(request, id)
      case ChangeSecret => secretUpdate(request, id)
      case MemberDelete => R.delete(id) >>= { noContentResponse(_, id) }
      case MemberUpdate => memberUpdate(request, id)
    }
}
