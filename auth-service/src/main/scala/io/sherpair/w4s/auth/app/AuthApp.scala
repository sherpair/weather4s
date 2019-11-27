package io.sherpair.w4s.auth.app

import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{AuthAction, Crypt, EmailType, Member, MemberRequest, SignupRequest, UniqueViolation}
import io.sherpair.w4s.auth.domain.AuthAction._
import io.sherpair.w4s.auth.domain.EmailType.Activation
import io.sherpair.w4s.auth.repository.RepositoryMemberOps
import io.sherpair.w4s.domain.Logger
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Request, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.common.SecureRandomId
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

class AuthApp[F[_]: Sync](
    auth: Authenticator[F])(
    implicit C: AuthConfig, E: EntityEncoder[F, Member], L: Logger[F], R: RepositoryMemberOps[F]
) extends Http4sDsl[F] {

  implicit val passwordHasher: PasswordHasher[F, Crypt] = Crypt.syncPasswordHasher

  implicit val memberRequestDecoder: EntityDecoder[F, MemberRequest] = jsonOf
  implicit val signupRequestDecoder: EntityDecoder[F, SignupRequest] = jsonOf

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case            GET -> Root / "account-activation" / tokenId => activation(tokenId)

    case request @ POST -> Root / "expired-token" => validateMember(request, ActivationExpired)

    case request @ POST -> Root / "signin" => validateMember(request, Signin)

    case request @ POST -> Root / "signup" => signup(request)
  }

  private def activation(tokenId: String): F[Response[F]] =
    auth.retrieveToken(SecureRandomId.coerce(tokenId)) >>= {
      _.fold(NotFound()) { token =>
        R.find(token.memberId) >>= {
          _.fold(NotFound()) {
            auth.deleteToken(token) *> enableMember(_)
          }
        }
      }
    }

  private def enableMember(member: Member): F[Response[F]] =
    if (member.active) Ok(member)
    else R.enable(member.id) >>= { enabled =>
      if (enabled > 0) Ok(member.copy(active = true))
      else L.error(s"${member} not enabled??") *> InternalServerError()
    }

      private def fieldId(accountId: String): String =
    if (accountId.indexOf('@') > 0 && accountId.count(_ == '@') == 1) "email" else "account_id"

  private def notFound(key: String): F[Response[F]] = NotFound(s"Member(${key}) is not known")

  private def resendTokenOnRateLimiting(member: Member, emailType: EmailType): F[Response[F]] =
    auth.deleteTokenIfOlderThan(C.token.rateLimit, member)
      .ifM(auth.sendToken(member, emailType) *> NoContent(), TooManyRequests(C.token.rateLimit.toString))

  private def resendActivationToken(member: Member): F[Response[F]] =
    if (member.active) NotAcceptable("Already active")
    else resendTokenOnRateLimiting(member, Activation)

  private def signinResponse(member: Member): F[Response[F]] =
    if (member.active) auth.addJwtToAuthorizationHeader(NoContent(), member) else Forbidden("Inactive")

  private def signup(request: Request[F]): F[Response[F]] =
    request.decode[SignupRequest] { signupRequest =>
      R.insert(signupRequest)
        .flatTap(auth.sendToken(_, Activation))
        .flatMap(member => Created(member))
        .recoverWith {
          case UniqueViolation(msg) => Conflict(msg)
        }
    }

  private def validateMember(request: Request[F], authAction: AuthAction): F[Response[F]] =
    request.decode[MemberRequest] { memberRequest =>
      val accountId = memberRequest.accountId
      R.findWithSecret(fieldId(accountId), accountId)
        .flatMap(_.fold(notFound(accountId))(validateRequest(_, memberRequest, authAction)))
    }

  private def validateRequest(
      memberWithSecret: (Member, String), memberRequest: MemberRequest, authAction: AuthAction
  ): F[Response[F]] =
    authAction match {
      case ActivationExpired => resendActivationToken(memberWithSecret._1)
      case Signin =>
        Crypt.checkpwBool[F](memberRequest.secret, PasswordHash[Crypt](memberWithSecret._2))
          .ifM(signinResponse(memberWithSecret._1), notFound(memberRequest.accountId))
    }
}
