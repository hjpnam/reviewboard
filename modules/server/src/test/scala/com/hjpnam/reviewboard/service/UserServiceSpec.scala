package com.hjpnam.reviewboard.service

import com.hjpnam.reviewboard.domain.data.{User, UserID, UserToken}
import com.hjpnam.reviewboard.domain.error.{ObjectNotFound, Unauthorized}
import com.hjpnam.reviewboard.fixture.RepoStub
import com.hjpnam.reviewboard.repository.{RecoveryTokenRepository, UserRepository}
import zio.test.*
import zio.test.Assertion.*
import zio.{Scope, Task, ZIO, ZLayer}

import scala.collection.mutable

object UserServiceSpec extends ZIOSpecDefault, RepoStub:
  val stubTokenRepoLayer = ZLayer.succeed(
    new RecoveryTokenRepository:
      val db = mutable.Map.empty[String, String]
      override def getToken(email: String): Task[Option[String]] =
        ZIO.attempt {
          val token = "RECOVERY"
          db += (email -> token)
          Some(token)
        }

      override def checkToken(email: String, token: String): Task[Boolean] =
        ZIO.attempt(db.get(email).contains(token))
  )

  val stubJWTServiceLayer = ZLayer.succeed(
    new JWTService:
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.email, "test-token", Long.MaxValue))
      override def verifyToken(token: String): Task[UserID] =
        ZIO.succeed(UserID(testUser.id, testUser.email))
  )

  val stubEmailServiceLayer = ZLayer.succeed(
    new EmailService:
      override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ZIO.unit

      override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] = ZIO.unit
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserServiceSpec")(
      test("create and validate a user") {
        for
          service <- ZIO.service[UserService]
          user    <- service.registerUser(testUser.email, "password")
          valid   <- service.verifyPassword(testUser.email, "password")
        yield assertTrue(valid) &&
          assert(user)(hasField("email", _.email, equalTo(testUser.email)))
      },
      test("invalidate incorrect credentials") {
        for
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(testUser.email, "foobar")
        yield assertTrue(!valid)
      },
      test("invalidate non-existent user") {
        for
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword("foo@bar.com", testUser.hashedPassword)
        yield assertTrue(!valid)
      },
      test("update password") {
        val newPassword = "newpassword"
        for
          service  <- ZIO.service[UserService]
          _        <- service.updatePassword(testUser.email, "password", newPassword)
          oldValid <- service.verifyPassword(testUser.email, "password")
          newValid <- service.verifyPassword(testUser.email, newPassword)
        yield assertTrue(newValid && !oldValid)
      },
      test("delete non-existent user") {
        for
          service <- ZIO.service[UserService]
          result  <- service.deleteUser("foo@bar.com", "password").exit
        yield assert(result)(failsWithA[ObjectNotFound])
      },
      test("delete a user with the wrong password") {
        for
          service <- ZIO.service[UserService]
          result  <- service.deleteUser(testUser.email, "foobar").exit
        yield assert(result)(failsWithA[Unauthorized])
      },
      test("delete a user") {
        for
          service <- ZIO.service[UserService]
          user    <- service.deleteUser(testUser.email, "password")
        yield assert(user)(hasField("email", _.email, equalTo(testUser.email)))
      },
      test("recover password") {
        val newPassword = "newpassword"
        for
          service <- ZIO.service[UserService]
          _       <- service.sendPasswordRecoveryToken(testEmail)
          _       <- service.recoverPassword(testEmail, "RECOVERY", newPassword)
          valid   <- service.verifyPassword(testEmail, newPassword)
        yield assertTrue(valid)
      }
    ).provide(
      UserService.live,
      stubJWTServiceLayer,
      stubEmailServiceLayer,
      stubUserRepoLayer,
      stubTokenRepoLayer
    )
