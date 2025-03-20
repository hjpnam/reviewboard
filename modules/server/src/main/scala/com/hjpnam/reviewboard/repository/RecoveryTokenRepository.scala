package com.hjpnam.reviewboard.repository

import com.hjpnam.reviewboard.config.{Configs, RecoveryTokenConfig}
import com.hjpnam.reviewboard.domain.data.PasswordRecoveryToken
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait RecoveryTokenRepository:
  def getToken(email: String): Task[Option[String]]
  def checkToken(email: String, token: String): Task[Boolean]

object RecoveryTokenRepository:
  val live = ZLayer(
    for
      config   <- ZIO.service[RecoveryTokenConfig]
      quill    <- ZIO.service[Quill.Postgres[SnakeCase.type]]
      userRepo <- ZIO.service[UserRepository]
    yield new RecoveryTokenRepositoryLive(config, quill, userRepo)
  )

  val configuredLive = Configs.makeLayer[RecoveryTokenConfig]("app.recoveryToken") >>> live

class RecoveryTokenRepositoryLive(
    tokenConfig: RecoveryTokenConfig,
    quill: Quill.Postgres[SnakeCase.type],
    userRepo: UserRepository
) extends RecoveryTokenRepository:
  import quill.*

  inline given schema: SchemaMeta[PasswordRecoveryToken] =
    schemaMeta[PasswordRecoveryToken]("recovery_token")
  inline given insMeta: InsertMeta[PasswordRecoveryToken] = insertMeta[PasswordRecoveryToken]()
  inline given updMeta: UpdateMeta[PasswordRecoveryToken] =
    updateMeta[PasswordRecoveryToken](_.email)

  private val tokenDuration = tokenConfig.duration

  override def getToken(email: String): Task[Option[String]] =
    userRepo.getByEmail(email).flatMap {
      case Some(_) => makeFreshToken(email).map(Some(_))
      case _       => ZIO.none
    }

  override def checkToken(email: String, token: String): Task[Boolean] =
    for
      now <- Clock.instant
      isValid <- run(
        query[PasswordRecoveryToken]
          .filter(r =>
            r.email == lift(email) && r.token == lift(token) && r.expiration > lift(
              now.toEpochMilli
            )
          )
          .map(_.email)
      ).map(_.nonEmpty)
    yield isValid

  private def findToken(email: String): Task[Option[String]] = run(
    query[PasswordRecoveryToken].filter(_.email == lift(email)).map(_.token)
  ).map(_.headOption)

  private def randomUppercaseString(len: Int): String =
    scala.util.Random.alphanumeric.take(len).mkString.toUpperCase

  private def replaceToken(email: String): Task[String] =
    val token = randomUppercaseString(8)
    run(
      query[PasswordRecoveryToken]
        .updateValue(
          lift(
            PasswordRecoveryToken(
              email,
              token,
              java.lang.System.currentTimeMillis() + tokenDuration
            )
          )
        )
        .returning(_.token)
    )

  private def generateToken(email: String): Task[String] =
    val token = randomUppercaseString(8)
    run(
      query[PasswordRecoveryToken]
        .insertValue(
          lift(
            PasswordRecoveryToken(
              email,
              token,
              java.lang.System.currentTimeMillis() + tokenDuration
            )
          )
        )
        .returning(_.token)
    )

  private def makeFreshToken(email: String): Task[String] =
    findToken(email).flatMap {
      case Some(_) => replaceToken(email)
      case _       => generateToken(email)
    }
