package com.hjpnam.reviewboard.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.hjpnam.reviewboard.config.{Configs, JWTConfig}
import com.hjpnam.reviewboard.domain.data.{User, UserID, UserToken}
import zio.{Clock, Task, TaskLayer, URLayer, ZIO, ZLayer}

trait JWTService:
  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserID]

object JWTService:
  val live: URLayer[JWTConfig, JWTServiceLive] = ZLayer {
    for
      jwtConfig <- ZIO.service[JWTConfig]
      clock     <- Clock.javaClock
    yield new JWTServiceLive(jwtConfig, clock)
  }

  val configuredLive: TaskLayer[JWTServiceLive] = Configs.makeLayer[JWTConfig]("app.jwt") >>> live

class JWTServiceLive(jwtConfig: JWTConfig, clock: java.time.Clock) extends JWTService:
  private val ISSUER         = "hjpnam.com"
  private val CLAIM_USERNAME = "username"

  private val algorithm = Algorithm.HMAC512(jwtConfig.secret)
  private val verifier: JWTVerifier = JWT
    .require(algorithm)
    .withIssuer(ISSUER)
    .asInstanceOf[BaseVerification]
    .build(clock)

  override def createToken(user: User): Task[UserToken] = for
    now <- ZIO.attempt(clock.instant())
    expiration = now.plusSeconds(jwtConfig.ttl)
    token <- ZIO.attempt(
      JWT
        .create()
        .withIssuer(ISSUER)
        .withIssuedAt(now)
        .withExpiresAt(expiration)
        .withSubject(user.id.toString)
        .withClaim(CLAIM_USERNAME, user.email)
        .sign(algorithm)
    )
  yield UserToken(user.email, token, expiration.getEpochSecond)

  override def verifyToken(token: String): Task[UserID] = for
    decoded <- ZIO.attempt(verifier.verify(token))
    userId <- ZIO.attempt(
      UserID(
        id = decoded.getSubject.toLong,
        email = decoded.getClaim(CLAIM_USERNAME).asString()
      )
    )
  yield userId
