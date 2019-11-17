package io.sherpair.w4s.auth.config

import java.util.Properties
import javax.mail.{Authenticator, PasswordAuthentication}

case class Smtp(properties: Properties, credentials: Authenticator)

object Smtp {

  def apply(): Smtp = {

    val credentials = new Authenticator {
      override val getPasswordAuthentication =
        new PasswordAuthentication(
          envVar("W4S_AUTH_SMTP_USER"),
          envVar("W4S_AUTH_SMTP_SECRET")
        )
    }

    val properties: Properties = {
      val properties = new Properties
      properties.put("mail.mime.allowutf8", "true")
      properties.put("mail.smtp.auth", "true")
      properties.put("mail.smtp.host", envVar("W4S_AUTH_SMTP_ADDRESS"))
      properties.put("mail.smtp.port", envVar("W4S_AUTH_SMTP_PORT"))
      properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
      properties.put("mail.smtp.ssl.enable", "true")
      properties.put("mail.smtp.starttls.enable", "true")
      properties.put("mail.transport.protocol", "smtp")
      properties
    }

    Smtp(properties, credentials)
  }

  private def envVar(name: String): String = sys.env.get(name).getOrElse(envVarUnset(name))

  private def envVarUnset(name: String): String =
    throw new NoSuchElementException(s"Env var '${name}' is not set but it's mandatory. Aborting...")
}
