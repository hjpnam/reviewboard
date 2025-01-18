package com.hjpnam.reviewboard.service

import com.hjpnam.reviewboard.config.{Configs, EmailConfig}
import zio.*

import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}

trait EmailService:
  def sendEmail(to: String, subject: String, content: String): Task[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] =
    val subject = "Password Recovery"
    val content =
      s"""
         |<div style="border: 1px solid black; padding: 20px; font-family: sans-serif; line-height: 2; font-size: 20px;">
         |  <h1>Password Recovery</h1>
         |  <p>Your password recovery token is: <strong>$token</strong>
         |</div>
         |""".stripMargin

    sendEmail(to, subject, content)

object EmailService:
  val live: URLayer[EmailConfig, EmailService] = EmailServiceLive.layer
  val configuredLive: TaskLayer[EmailService]  = EmailServiceLive.configuredLayer

class EmailServiceLive private (emailConfig: EmailConfig) extends EmailService:
  private val host: String     = emailConfig.host
  private val port: Int        = emailConfig.port
  private val user: String     = emailConfig.user
  private val password: String = emailConfig.password
  private val propsResource: UIO[Properties] =
    val prop = new Properties()
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", "true")
    prop.put("mail.smtp.host", host)
    prop.put("mail.smtp.port", port)
    prop.put("mail.smtp.ssl.trust", host)
    ZIO.succeed(prop)

  override def sendEmail(to: String, subject: String, content: String): Task[Unit] =
    val messageZIO = for
      prop    <- propsResource
      session <- createSession(prop)
      message <- createMessage(session)("peter@hjpnam.com", to, subject, content)
    yield message

    messageZIO.map(Transport.send)

  private def createSession(prop: Properties): Task[Session] = ZIO.succeed(
    Session.getInstance(
      prop,
      new Authenticator {
        override def getPasswordAuthentication: PasswordAuthentication =
          new PasswordAuthentication(user, password)
      }
    )
  )

  private def createMessage(
      session: Session
  )(from: String, to: String, subject: String, content: String): UIO[MimeMessage] =
    val message = new MimeMessage(session)
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    ZIO.succeed(message)

object EmailServiceLive:
  val layer: URLayer[EmailConfig, EmailServiceLive] = ZLayer(
    for emailConfig <- ZIO.service[EmailConfig]
    yield new EmailServiceLive(emailConfig)
  )

  val configuredLayer: TaskLayer[EmailServiceLive] =
    Configs.makeLayer[EmailConfig]("app.email") >>> layer
