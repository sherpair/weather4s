package io.sherpair.w4s.auth.config

import java.net.InetAddress
import javax.mail.{Session, Transport}
import javax.mail.Message.RecipientType
import javax.mail.internet.MimeMessage

import cats.syntax.either._
import cats.syntax.option._
import io.sherpair.w4s.auth.domain.{EmailType, Member, Token}
import io.sherpair.w4s.config.Configuration
import io.sherpair.w4s.domain.Logger
import tsec.common.SecureRandomId

abstract class MaybePostman(implicit C: Configuration) {

  lazy val path = s"${C.plainHttp.fold("https")(if (_) "http" else "https")}://${host}${C.root}"

  def sendEmail(token: Token, member: Member, emailType: EmailType): Option[String] = none[String]

  protected def url(segment: String): String = s"${path}/${segment}"

  protected def url(segment: String, token: SecureRandomId): String = s"${path}/${segment}/${token}"

  private val host: String = s"${InetAddress.getLocalHost.getHostAddress}:${C.host.port}"
}

class Postman[F[_]](
    smtp: Smtp, templates: Map[String, String])(
    implicit C: Configuration, L: Logger[F]
) extends MaybePostman {

  override def sendEmail(token: Token, member: Member, emailType: EmailType): Option[String] =
    templates.get(emailType.reason).fold(None) { template =>
      val session = Session.getInstance(smtp.properties, smtp.credentials)
      val message = new MimeMessage(session)
      message.setFrom("Weather4s")
      message.setRecipients(RecipientType.TO, member.email);
      message.setSubject(emailType.reason)
      message.setContent(content(template, emailType, member, token), "text/html")
      val log = s"email to ${member}. Token's url was ${url(emailType.segment)}"
      Either.catchNonFatal(Transport.send(message)).fold(
        L.error(_)(s"While sending ${log}"), _ => L.info(s"Sent ${log}")
      )
      None
    }

  private def content(template: String, emailType: EmailType, member: Member, token: Token): String =
    template
      .replaceFirst("==firstName==", member.firstName)
      .replaceFirst("==token-email-url==", url(emailType.segment, token.tokenId))
}
