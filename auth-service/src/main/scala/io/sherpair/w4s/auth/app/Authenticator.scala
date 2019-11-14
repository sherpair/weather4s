package io.sherpair.w4s.auth.app

import java.security.PrivateKey
import java.time.Instant
import javax.mail.Message.RecipientType
import javax.mail.Session
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
import io.sherpair.w4s.auth.domain.{EmailType, Member, Token}
import io.sherpair.w4s.auth.repository.{RepositoryMemberOps, RepositoryTokenOps}
import io.sherpair.w4s.domain.{unit, Logger}
import org.http4s.{AuthScheme, Credentials, Response}
import org.http4s.headers.Authorization
import pdi.jwt.{Jwt, JwtClaim, JwtTime}
import pdi.jwt.algorithms.JwtRSAAlgorithm
import tsec.common.SecureRandomId

class Authenticator[F[_]](
    jwtAlgorithm: JwtRSAAlgorithm, privateKey: PrivateKey)(
    implicit C: AuthConfig, L: Logger[F], R: RepositoryTokenOps[F], S: Sync[F]
) {

  def addJwtToAuthorizationHeader(responseF: F[Response[F]], member: Member): F[Response[F]] =
    for {
      response <- responseF
      jwt <- S.delay(Jwt.encode(claims(member), privateKey, jwtAlgorithm))
    }
    yield response.putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, jwt)))

  def deleteToken(token: Token): F[Unit] =
    R.delete(token.tokenId).handleErrorWith {
      L.error(_)(s"While trying to delete a token for Member(${token.memberId})")
    }

  def deleteTokenIfOlderThan(rateLimit: FiniteDuration, member: Member): F[Boolean] =
    R.deleteIfOlderThan(rateLimit, member)

  def retrieveToken(tokenId: SecureRandomId): F[Option[Token]] =
    R.find(tokenId) >>= {
      _.fold(none[Token].pure[F]) { token =>
        S.delay(if (token.expiryDate.isBefore(Instant.now)) none[Token] else token.some)
      }
    }

  def sendToken(member: Member, emailType: EmailType): F[Unit] =
    for {
      now <- S.delay(Instant.now())
      expiryDate = now.plusSeconds(C.token.duration.toSeconds)
      token <- R.insert(Token(SecureRandomId.Strong.generate, member.id, expiryDate))
      _ <- sendEmail(token, member, emailType)
    }
      yield unit

  private def claims(member: Member): JwtClaim = {
    val now = JwtTime.nowSeconds
    JwtClaim(
      member.claimContent, Claims.issO, member.id.toString.some, Claims.audO,
      (now + C.authToken.duration.toSeconds).some, now.some, now.some
    )
  }

  private def content(template: String, token: SecureRandomId, member: Member): String =
    template
      .replaceFirst("==firstName==", member.firstName)
      .replaceFirst("==email url protocol==", C.plainHttp.fold("https")(if (_) "http" else "https"))
      .replaceFirst("==email url host==", C.host.joined)
      .replaceFirst("==email url path==", C.root)
      .replaceFirst("==email url token==", token)

  private def loadTemplate(template: String): F[String] =
    Resource
      .fromAutoCloseable(S.delay(fromResource(template)))
      .use(_.mkString.pure[F])

  private def sendEmail(token: Token, member: Member, emailType: EmailType): F[Unit] =
    loadTemplate(emailType.template).map { template =>
      C.smtp.fold(unit) { smtp =>  // Unless due to a bug, C.smtp is never None as it was set at program start.
        val session = Session.getInstance(smtp.properties, smtp.credentials)
        val message = new MimeMessage(session)
        message.setFrom("Weather4s")
        message.setRecipients(RecipientType.TO, member.email);
        message.setSubject(emailType.reason)
        message.setContent(content(template, token.tokenId, member), "text/html")
        smtp.transporter.send(message)
      }
    }
}

object Authenticator {

  def apply[F[_]](
      jwtAlgorithm: JwtRSAAlgorithm, privateKey: PrivateKey)(
      implicit C: AuthConfig, L: Logger[F], R: RepositoryTokenOps[F], RU: RepositoryMemberOps[F], S: Sync[F]
  ): Authenticator[F] =
    new Authenticator[F](jwtAlgorithm, privateKey)
}

