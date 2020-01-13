package io.sherpair.w4s.auth.app

import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{
  AuthAction, Crypt, EmailType, Kind, Member, MemberRequest, SignupRequest, UniqueViolation
}
import io.sherpair.w4s.auth.domain.AuthAction._
import io.sherpair.w4s.auth.repository.RepositoryMemberOps
import io.sherpair.w4s.domain.Logger
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Request, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.common.SecureRandomId
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

class AuthApp[F[_]](tokenOps: TokenOps[F])(
    implicit C: AuthConfig, E: EntityEncoder[F, Member], L: Logger[F], R: RepositoryMemberOps[F], S: Sync[F]
) extends Http4sDsl[F] {

  implicit val passwordHasher: PasswordHasher[F, Crypt] = Crypt.syncPasswordHasher

  implicit val memberRequestDecoder: EntityDecoder[F, MemberRequest] = jsonOf
  implicit val signupRequestDecoder: EntityDecoder[F, SignupRequest] = jsonOf

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case            GET -> Root / "account-activation" / tokenId => activation(tokenId, Kind.Activation)

    case            GET -> Root / "change-email-confirmed" / tokenId => activation(tokenId, Kind.ChangeEMail)

    case request @ POST -> Root / "activation-expired" => validateMember(request, ActivationExpired)

    case request @ POST -> Root / "change-email-expired" => validateMember(request, ChangeEMailExpired)

    case request @ POST -> Root / "signin" => validateMember(request, Signin)

    case request @ POST -> Root / "signup" => signup(request)
  }

  private def activation(tokenId: String, kind: Kind): F[Response[F]] =
    tokenOps.retrieve(SecureRandomId.coerce(tokenId), kind) >>= {
      _.fold(NotFound()) { token =>
        R.find(token.memberId) >>= {
          _.fold(NotFound()) {
            tokenOps.delete(token) *> enableMember(_)
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

  private def resendToken(member: Member, kind: Kind, emailType: EmailType): F[Response[F]] =
    if (member.active) NotAcceptable("Already active")
    else resendTokenOnRateLimiting(member, kind, emailType)

  private def resendTokenOnRateLimiting(member: Member, kind: Kind, emailType: EmailType): F[Response[F]] =
    tokenOps.deleteIfOlderThan(C.token.rateLimit, member, kind)
      .ifM(
        tokenOps.send(member, kind, emailType) *> NoContent(),
        TooManyRequests(C.token.rateLimit.toString)
      )

  private def signinResponse(member: Member): F[Response[F]] =
    if (member.active) tokenOps.addTokensToResponse(member, NoContent()) else Forbidden("Inactive")

  private def signup(request: Request[F]): F[Response[F]] =
    request.decode[SignupRequest] { signupRequest =>
      S.delay(signupRequest.hasLegalSecret).ifM(
        R.insert(signupRequest)
          .flatTap(tokenOps.send(_, Kind.Activation, EmailType.Activation))
          .flatMap(Created(_))
          .recoverWith {
            case UniqueViolation(msg) => Conflict(msg)
          },

        NotAcceptable(signupRequest.illegalSecret)
      )
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
      case ActivationExpired => resendToken(memberWithSecret._1, Kind.Activation, EmailType.Activation)
      case ChangeEMailExpired => resendToken(memberWithSecret._1, Kind.ChangeEMail, EmailType.ChangeEMail)
      case Signin =>
        Crypt.checkpwBool[F](memberRequest.secret, PasswordHash[Crypt](memberWithSecret._2))
          .ifM(signinResponse(memberWithSecret._1), notFound(memberRequest.accountId))
    }
}
