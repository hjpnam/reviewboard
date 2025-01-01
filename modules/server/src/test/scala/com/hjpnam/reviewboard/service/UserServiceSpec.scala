package com.hjpnam.reviewboard.service

import com.hjpnam.reviewboard.domain.data.{User, UserID, UserToken}
import com.hjpnam.reviewboard.domain.error.{ObjectNotFound, Unauthorized}
import com.hjpnam.reviewboard.repository.UserRepository
import zio.test.*
import zio.test.Assertion.*
import zio.{Scope, Task, ZIO, ZLayer}

import scala.collection.mutable

object UserServiceSpec extends ZIOSpecDefault:
  val testUser = User(
    1L,
    "test@email.com",
    "1000:da70ffa630dc4793f0e4a64a8ca1aa1ae5892a29da5ce97d:2d1538385faf4154231d6fae95e09306efd841b32e9eb0bc"
  )
  val stubUserRepoLayer = ZLayer.succeed(
    new UserRepository:
      val db = mutable.Map(1L -> testUser)
      override def create(user: User): Task[User] = ZIO.succeed {
        db += (user.id -> user)
        user
      }

      override def update(id: Long, op: User => User): Task[User] = ZIO.succeed {
        val newUser = op(db(id))
        db += (newUser.id -> newUser)
        newUser
      }

      override def getById(id: Long): Task[Option[User]] = ZIO.succeed(db.get(id))

      override def getByEmail(email: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.email == email))

      override def delete(id: Long): Task[User] = ZIO.succeed {
        val user = db(id)
        db -= id
        user
      }
  )

  val stubJWTServiceLayer = ZLayer.succeed(
    new JWTService:
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.email, "test-token", Long.MaxValue))
      override def verifyToken(token: String): Task[UserID] =
        ZIO.succeed(UserID(testUser.id, testUser.email))
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
      }
    ).provide(UserService.live, stubUserRepoLayer, stubJWTServiceLayer)
