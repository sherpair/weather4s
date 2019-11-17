package io.sherpair.w4s.auth.app

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{AuthAction, Crypt, EmailType, Member, MemberRequest, SignupRequest}
import io.sherpair.w4s.auth.domain.AuthAction._
import io.sherpair.w4s.auth.domain.EmailType.Activation
import io.sherpair.w4s.auth.repository.RepositoryMemberOps
import io.sherpair.w4s.domain.{Logger, W4sError}
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Request, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.common.SecureRandomId
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

class AuthApp[F[_]: Sync](
    A: Authenticator[F])(
    implicit C: AuthConfig, E: EntityEncoder[F, Member], L: Logger[F], R: RepositoryMemberOps[F]
) extends Http4sDsl[F] {

  implicit val passwordHasher: PasswordHasher[F, Crypt] = Crypt.syncPasswordHasher

  implicit val loginDecoder: EntityDecoder[F, MemberRequest] = jsonOf
  implicit val registrationDecoder: EntityDecoder[F, SignupRequest] = jsonOf

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case            GET -> Root / "account-activation" / tokenId => activation(tokenId)

    case request @ POST -> Root / "expired-token" => validateMember(request, ActivationExpired)

    /* TODO */
    case request @ POST -> Root / "reset-secret" => validateMember(request, ResetSecret)

    case request @ POST -> Root / "signin" => validateMember(request, Signin)

    case request @ POST -> Root / "signup" => signup(request)
  }

  private def activation(tokenId: String): F[Response[F]] =
    A.retrieveToken(SecureRandomId.coerce(tokenId)) >>= {
      _.fold(NotFound()) { token =>
        R.find(token.memberId) >>= {
          _.fold(NotFound()) { member =>
            (A.deleteToken(token) *> R.enable(member.id)).whenA(!member.active) >> Ok(member)
          }
        }
      }
    }

  private def fieldId(accountId: String): String =
    if (accountId.indexOf('@') > 0 && accountId.count(_ == '@') == 1) "email" else "account_id"

  private def notFound(key: String): F[Response[F]] = NotFound(s"Member(${key}) is not known")

  private def resendTokenOnRateLimiting(member: Member, emailType: EmailType): F[Response[F]] =
    A.deleteTokenIfOlderThan(C.token.rateLimit, member)
      .ifM(A.sendToken(member, emailType) *> Ok(), TooManyRequests(C.token.rateLimit.toString))

  private def resendActivationToken(member: Member): F[Response[F]] =
    if (member.active) NotAcceptable("Already active")
    else resendTokenOnRateLimiting(member, Activation)

  private def sendResetSecretToken(member: Member): F[Response[F]] =
    if (member.active) resendTokenOnRateLimiting(member, EmailType.ResetSecret)
    else Forbidden("Inactive")

  private def signinResponse(member: Member): F[Response[F]] =
    if (member.active) A.addJwtToAuthorizationHeader(Ok(), member) else Forbidden("Inactive")

  private def signup(request: Request[F]): F[Response[F]] =
    request.decode[SignupRequest] { signupRequest =>
      R.insert(signupRequest)
        .flatTap(A.sendToken(_, Activation))
        .flatMap(member => Created(member))
        .handleErrorWith {
          case W4sError(msg, _) => Conflict(msg)
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
      case ResetSecret => sendResetSecretToken(memberWithSecret._1)
      case Signin =>
        Crypt.checkpwBool[F](memberRequest.secret, PasswordHash[Crypt](memberWithSecret._2))
          .ifM(signinResponse(memberWithSecret._1), notFound(memberRequest.accountId))
    }
}
