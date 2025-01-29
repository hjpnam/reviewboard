package com.hjpnam.reviewboard.service

import com.hjpnam.reviewboard.config.JWTConfig
import com.hjpnam.reviewboard.domain.data.User
import com.hjpnam.reviewboard.util.TestGen
import zio.test.*
import zio.test.Assertion.*
import zio.{Scope, ZIO, ZLayer}

object JWTServiceSpec extends ZIOSpecDefault with TestGen:
  private def genUser: User =
    User(
      id = genLong(),
      email = s"${genString(5)}@${genString(5)}.com",
      hashedPassword = genString(8)
    )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("JWTServiceSpec")(
      test("create and validate token") {
        val user = genUser
        for
          service   <- ZIO.service[JWTService]
          userToken <- service.createToken(user)
          userId    <- service.verifyToken(userToken.token)
        yield assert(userId)(hasField("id", _.id, equalTo(user.id))) &&
          assert(userId)(hasField("email", _.email, equalTo(user.email)))
      }
    ).provide(JWTService.live, ZLayer.succeed(JWTConfig("secret", 3600L)))
