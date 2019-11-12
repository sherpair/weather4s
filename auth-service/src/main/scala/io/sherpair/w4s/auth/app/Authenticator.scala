package io.sherpair.w4s.auth.app

import java.security.PrivateKey
import java.time.Instant
import java.util.Properties
import javax.mail.{PasswordAuthentication, Session, Transport}
import javax.mail.Message.RecipientType
import javax.mail.internet.MimeMessage

import scala.concurrent.duration.FiniteDuration
import scala.io.Source.fromResource

import cats.effect.{Resource, Sync}
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import io.sherpair.w4s.auth.{clock, Claims}
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{Token, User}
import io.sherpair.w4s.auth.repository.{RepositoryTokenOps, RepositoryUserOps}
import io.sherpair.w4s.domain.{unit, Logger, W4sError}
import org.http4s.{AuthScheme, Credentials, Response}
import org.http4s.headers.Authorization
import pdi.jwt.{Jwt, JwtClaim, JwtTime}
import pdi.jwt.algorithms.JwtRSAAlgorithm
import tsec.common.SecureRandomId

class Authenticator[F[_]](
    jwtAlgorithm: JwtRSAAlgorithm, privateKey: PrivateKey)(
    implicit C: AuthConfig, L: Logger[F], R: RepositoryTokenOps[F], S: Sync[F]
) {

  def addJwtToAuthorizationHeader(responseF: F[Response[F]], user: User): F[Response[F]] =
    for {
      response <- responseF
      jwt <- S.delay(Jwt.encode(claims(user), privateKey, jwtAlgorithm))
    }
    yield response.putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, jwt)))

  def deleteToken(token: Token): F[Unit] =
    R.delete(token.tokenId).handleErrorWith {
      L.error(_)(s"While trying to delete a token for User(${token.userId})")
    }

  def deleteTokenIfOlderThan(rateLimit: FiniteDuration, user: User): F[Boolean] =
    R.deleteIfOlderThan(rateLimit, user)

  def retrieveToken(tokenId: SecureRandomId): F[Option[Token]] =
    R.find(tokenId) >>= {
      _.fold(none[Token].pure[F]) { token =>
        S.delay(if (token.expiryDate.isBefore(Instant.now)) none[Token] else token.some)
      }
    }

  def sendToken(user: User): F[Unit] =
    for {
      now <- S.delay(Instant.now())
      expiryDate = now.plusSeconds(C.token.duration.toSeconds)
      token <- R.insert(Token(SecureRandomId.Strong.generate, user.id, expiryDate))
      _ <- sendEmail(token, user)
    }
      yield unit

  private def claims(user: User): JwtClaim = {
    val now = JwtTime.nowSeconds
    JwtClaim(
      user.claimContent, Claims.issO, user.id.toString.some, Claims.audO,
      (now + C.authToken.duration.toSeconds).some, now.some, now.some
    )
  }

  private def content(template: String, token: SecureRandomId, user: User): String =
    template
      .replaceFirst("==firstName==", user.firstName)
      .replaceFirst("==email url protocol==", if (C.plainHttp) "http" else "https")
      .replaceFirst("==email url host==", C.host.joined)
      .replaceFirst("==email url path==", C.root)
      .replaceFirst("==email url token==", token)

  private lazy val emailTemplate: F[String] =
    Resource
      .fromAutoCloseable(S.delay(fromResource("activation.html")))
      .use(_.mkString.pure[F])

  // scalastyle:off throwerror
  private def envVarUnset(id: String): String =
    throw new W4sError(s"""Env var "${id}" is not set but it's mandatory. Aborting...""")
  // scalastyle:on throwerror

  /* Not a lazy val. The 2 env vars are mandatory... if they are not set the auth-service is aborted. */
  private val passwordAuthentication = new PasswordAuthentication(
    sys.env.get("W4S_AUTH_SMTP_USER").getOrElse(envVarUnset("W4S_AUTH_SMTP_USER")),
    sys.env.get("W4S_AUTH_SMTP_SECRET").getOrElse(envVarUnset("W4S_AUTH_SMTP_SECRET"))
  )

  private def sendEmail(token: Token, user: User): F[Unit] =
    emailTemplate.map { template =>
      val session = Session.getInstance(smtpProperties, smtpCredentials)
      val message = new MimeMessage(session)
      message.setFrom("Weather4s")
      message.setRecipients(RecipientType.TO, user.email);
      message.setSubject("Account activation")
      message.setContent(content(template, token.tokenId, user), "text/html")
      Transport.send(message)
    }

  private lazy val smtpCredentials = new javax.mail.Authenticator {
    override val getPasswordAuthentication = passwordAuthentication
  }

  private lazy val smtpProperties: Properties = {
    val properties = new Properties
    properties.put("mail.mime.allowutf8", "true")
    properties.put("mail.smtp.auth", "true")
    properties.put("mail.smtp.host", C.smtp.address)
    properties.put("mail.smtp.port", C.smtp.port)
    properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
    properties.put("mail.smtp.ssl.enable", "true")
    properties.put("mail.smtp.starttls.enable", "true")
    properties.put("mail.transport.protocol", "smtp")
    properties
  }
}

object Authenticator {

  def apply[F[_]](
      jwtAlgorithm: JwtRSAAlgorithm, privateKey: PrivateKey)(
      implicit C: AuthConfig, L: Logger[F], R: RepositoryTokenOps[F], RU: RepositoryUserOps[F], S: Sync[F]
  ): Authenticator[F] =
    new Authenticator[F](jwtAlgorithm, privateKey)
}
