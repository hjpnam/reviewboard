package com.hjpnam.reviewboard.http.controller

import com.auth0.jwt.exceptions.JWTVerificationException
import com.hjpnam.reviewboard.domain.data.{User, UserID, UserToken}
import com.hjpnam.reviewboard.http.controller.util.BackendStub
import com.hjpnam.reviewboard.http.request.{
  LoginRequest,
  RegisterUserRequest,
  UpdatePasswordRequest
}
import com.hjpnam.reviewboard.http.response.UserResponse
import com.hjpnam.reviewboard.service.{JWTService, UserService}
import zio.test.*
import zio.test.Assertion.*
import sttp.client3.*
import zio.json.*
import zio.{Scope, Task, ZIO, ZLayer}

object UserControllerSpec extends ZIOSpecDefault, BackendStub:
  private val testUser = User(
    id = 1L,
    email = "test@example.com",
    hashedPassword = "test-password"
  )
  private val userServiceStub = ZLayer.succeed(new UserService {
    override def registerUser(email: String, password: String): Task[User] = ZIO.succeed(testUser)

    override def verifyPassword(email: String, password: String): Task[Boolean] =
      ZIO.succeed(email == testUser.email)

    override def updatePassword(
        email: String,
        oldPassword: String,
        newPassword: String
    ): Task[User] = ZIO.succeed(testUser.copy(email = email, hashedPassword = newPassword))

    override def deleteUser(email: String, password: String): Task[User] = ZIO.succeed(testUser)

    override def generateToken(email: String, password: String): Task[Option[UserToken]] = ZIO.when(
      email == testUser.email && password == testUser.hashedPassword
    )(ZIO.succeed(UserToken(testUser.email, "test-token", 3600L)))
  })

  private val jwtServiceStub = ZLayer.succeed(new JWTService {
    override def createToken(user: User): Task[UserToken] = ???

    override def verifyToken(token: String): Task[UserID] =
      ZIO.cond(
        token == "valid-token",
        UserID(testUser.id, testUser.email),
        JWTVerificationException("invalid token")
      )
  })

  private val controllerBackendStubZIO = backendStubZIO(UserController.makeZIO)

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserControllerSpec")(
      test("POST /user") {
        val request = RegisterUserRequest(testUser.email, testUser.hashedPassword)

        for
          backendStub <- controllerBackendStubZIO(_.createUser :: Nil)
          response <- basicRequest
            .post(uri"/user")
            .body(request.toJson)
            .send(backendStub)
        yield assert(response.body.flatMap(_.fromJson[UserResponse]))(
          isRight(equalTo(UserResponse(testUser.email)))
        )
      },
      test("POST /user/login") {
        val request = LoginRequest(testUser.email, testUser.hashedPassword)
        for
          backendStub <- controllerBackendStubZIO(_.login :: Nil)
          response <- basicRequest
            .post(uri"/user/login")
            .body(request.toJson)
            .send(backendStub)
        yield assert(response.body.flatMap(_.fromJson[UserToken]))(
          isRight(equalTo(UserToken(testUser.email, "test-token", 3600L)))
        )
      },
      test("POST /user/login with wrong credentials") {
        val request = LoginRequest("foo@bar.com", "foobar")
        for
          backendStub <- controllerBackendStubZIO(_.login :: Nil)
          response <- basicRequest
            .post(uri"/user/login")
            .body(request.toJson)
            .send(backendStub)
        yield assert(response.body)(isLeft(equalTo("failed to login")))
      },
      test("PUT /user/password") {
        val request = UpdatePasswordRequest(testUser.email, testUser.hashedPassword, "new-password")
        for
          backendStub <- controllerBackendStubZIO(_.updatePassword :: Nil)
          response <- basicRequest
            .put(uri"/user/password")
            .body(request.toJson)
            .auth
            .bearer("valid-token")
            .send(backendStub)
        yield assert(response.body.flatMap(_.fromJson[UserResponse]))(
          isRight(equalTo(UserResponse(testUser.email)))
        )
      },
      test("PUT /user/password without bearer token") {
        val request = UpdatePasswordRequest(testUser.email, testUser.hashedPassword, "new-password")
        for
          backendStub <- controllerBackendStubZIO(_.updatePassword :: Nil)
          response <- basicRequest
            .put(uri"/user/password")
            .body(request.toJson)
            .send(backendStub)
        yield assert(response.body)(isLeft)
      },
      test("PUT /user/password with an invalid bearer token") {
        val request = UpdatePasswordRequest(testUser.email, testUser.hashedPassword, "new-password")
        for
          backendStub <- controllerBackendStubZIO(_.updatePassword :: Nil)
          response <- basicRequest
            .put(uri"/user/password")
            .auth
            .bearer("invalid-token")
            .body(request.toJson)
            .send(backendStub)
        yield assert(response.body)(isLeft(equalTo("invalid token")))
      }
    ).provide(userServiceStub, jwtServiceStub)
