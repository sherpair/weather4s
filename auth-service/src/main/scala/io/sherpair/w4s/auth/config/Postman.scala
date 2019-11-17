package io.sherpair.w4s.auth.config

import javax.mail.{Session, Transport}
import javax.mail.Message.RecipientType
import javax.mail.internet.MimeMessage

import cats.syntax.option._
import io.sherpair.w4s.auth.domain.{EmailType, Member, Token}
import io.sherpair.w4s.config.Configuration
import tsec.common.SecureRandomId

abstract class MaybePostman(implicit C: Configuration) {

  lazy val path = s"${C.plainHttp.fold("https")(if (_) "http" else "https")}://${C.host.joined}${C.root}"

  def sendEmail(token: Token, member: Member, emailType: EmailType): Option[String] = none[String]

  protected def url(segment: String, token: SecureRandomId): String = s"${path}/${segment}/${token}"
}

class Postman(smtp: Smtp, templates: Map[String, String])(implicit C: Configuration) extends MaybePostman {

  override def sendEmail(token: Token, member: Member, emailType: EmailType): Option[String] =
    templates.get(emailType.reason).fold(None) { template =>
      val session = Session.getInstance(smtp.properties, smtp.credentials)
      val message = new MimeMessage(session)
      message.setFrom("Weather4s")
      message.setRecipients(RecipientType.TO, member.email);
      message.setSubject(emailType.reason)
      message.setContent(content(template, emailType, member, token), "text/html")
      Transport.send(message)
      None
    }

  private def content(template: String, emailType: EmailType, member: Member, token: Token): String =
    template
      .replaceFirst("==firstName==", member.firstName)
      .replaceFirst("==token-email-url==", url(emailType.segment, token.tokenId))
}
