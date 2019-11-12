package io.sherpair.w4s.auth.app

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{AuthAction, SignupRequest, User, UserRequest}
import io.sherpair.w4s.auth.domain.AuthAction._
import io.sherpair.w4s.auth.repository.RepositoryUserOps
import io.sherpair.w4s.domain.{Logger, W4sError}
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Request, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.common.SecureRandomId
import tsec.passwordhashers.{PasswordHash, PasswordHasher}
import tsec.passwordhashers.jca.JCAPasswordPlatform

class AuthApp[F[_]: Sync, A](
    A: Authenticator[F], jca: JCAPasswordPlatform[A])(
    implicit C: AuthConfig, E: EntityEncoder[F, User], L: Logger[F], R: RepositoryUserOps[F]
) extends Http4sDsl[F] {

  implicit val passwordHasher: PasswordHasher[F, A] = jca.syncPasswordHasher

  implicit val loginDecoder: EntityDecoder[F, UserRequest] = jsonOf
  implicit val registrationDecoder: EntityDecoder[F, SignupRequest] = jsonOf

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case            GET -> Root / "activation-token" / tokenId => activation(tokenId)

    case request @ POST -> Root / "expired-token" => validateUser(request, ActivationExpired)

    case request @ POST -> Root / "reset-token" => validateUser(request, ResetSecret)

    case request @ POST -> Root / "signin" => validateUser(request, Signin)

    case request @ POST -> Root / "signup" => signup(request)
  }

  private def activation(tokenId: String): F[Response[F]] =
    A.retrieveToken(SecureRandomId.coerce(tokenId)) >>= {
      _.fold(NotFound()) { token =>
        R.find(token.userId) >>= {
          _.fold(NotFound()) { user =>
            (A.deleteToken(token) *> R.enable(user.id)).whenA(!user.active) >> Ok()
          }
        }
      }
    }

  private def fieldId(accountId: String): String =
    if (accountId.indexOf('@') > 0 && accountId.count(_ == '@') == 1) "email" else "account_id"

  private def notFound(key: String): F[Response[F]] = NotFound(s"User(${key}) is not known")

  private def resendTokenOnRateLimiting(user: User): F[Response[F]] =
    A.deleteTokenIfOlderThan(C.token.rateLimit, user)
      .ifM(A.sendToken(user) *> Ok(), TooManyRequests(C.token.rateLimit.toString))

  private def resendActivationToken(user: User): F[Response[F]] =
    if (user.active) NotAcceptable("Already active") else resendTokenOnRateLimiting(user)

  private def sendResetPasswordToken(user: User): F[Response[F]] =
    if (user.active) resendTokenOnRateLimiting(user) else Forbidden("Inactive")

  private def signinResponse(user: User): F[Response[F]] =
    if (user.active) A.addJwtToAuthorizationHeader(Ok(), user) else Forbidden("Inactive")

  private def signup(request: Request[F]): F[Response[F]] =
    request.decode[SignupRequest] { signupRequest =>
      jca.hashpw(signupRequest.secret) >>= {
        R.insert(signupRequest, _)
          .flatTap(A.sendToken)
          .flatMap(user => Created(user))
          .handleErrorWith {
            case W4sError(msg, _) => Conflict(msg)
          }
      }
    }

  private def validateRequest(
      userWithSecret: (User, String), userRequest: UserRequest, authAction: AuthAction
  ): F[Response[F]] =
    authAction match {
      case ActivationExpired => resendActivationToken(userWithSecret._1)
      case ResetSecret => sendResetPasswordToken(userWithSecret._1)
      case Signin =>
        jca.checkpwBool[F](userRequest.bytes, PasswordHash[A](userWithSecret._2))
          .ifM(signinResponse(userWithSecret._1), notFound(userRequest.accountId))
    }

  private def validateUser(request: Request[F], authAction: AuthAction): F[Response[F]] =
    request.decode[UserRequest] { userRequest =>
      val accountId = userRequest.accountId
      R.findForSignin(fieldId(accountId), accountId)
        .flatMap(_.fold(notFound(accountId))(validateRequest(_, userRequest, authAction)))
    }
}
