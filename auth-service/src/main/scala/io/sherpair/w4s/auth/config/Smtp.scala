package io.sherpair.w4s.auth.config

import java.util.Properties
import javax.mail.{Authenticator, Message, PasswordAuthentication, Transport}

case class SmtpEnv(host: String, port: String, user: String, secret: String, transporter: Transporter)

case class Smtp(properties: Properties, credentials: Authenticator, transporter: Transporter)

object Smtp {

  def apply(env: SmtpEnv): Smtp = {

    val credentials = new Authenticator {
      override val getPasswordAuthentication = new PasswordAuthentication(env.user, env.secret)
    }

    val properties: Properties = {
      val properties = new Properties
      properties.put("mail.mime.allowutf8", "true")
      properties.put("mail.smtp.auth", "true")
      properties.put("mail.smtp.host", env.host)
      properties.put("mail.smtp.port", env.port)
      properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
      properties.put("mail.smtp.ssl.enable", "true")
      properties.put("mail.smtp.starttls.enable", "true")
      properties.put("mail.transport.protocol", "smtp")
      properties
    }

    Smtp(properties, credentials, env.transporter)
  }
}

class Transporter {
  def send(message: Message): Unit = Transport.send(message)
}
