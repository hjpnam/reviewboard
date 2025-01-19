package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.domain.data.UserToken
import com.hjpnam.reviewboard.fixture.{ServiceStub, TestObject}
import com.hjpnam.reviewboard.http.controller.util.BackendStub
import com.hjpnam.reviewboard.http.request.{LoginRequest, RegisterUserRequest, UpdatePasswordRequest}
import com.hjpnam.reviewboard.http.response.UserResponse
import com.hjpnam.reviewboard.service.{JWTService, UserService}
import sttp.client3.*
import sttp.tapir.server.ServerEndpoint
import zio.json.*
import zio.test.*
import zio.test.Assertion.*
import zio.{Scope, Task, ZIO, ZLayer}

object UserControllerSpec extends ZIOSpecDefault, BackendStub, TestObject, ServiceStub:
  private val controllerBackendStubZIO: (
      UserController => List[ServerEndpoint[Any, Task]]
  ) => ZIO[JWTService & UserService, Nothing, SttpBackend[Task, Nothing]] = backendStubZIO(
    UserController.makeZIO
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    import com.hjpnam.reviewboard.util.RichSttpBackend.*

    suite("UserControllerSpec")(
      test("POST /user") {
        val request = RegisterUserRequest(testEmail, testUser.hashedPassword)
        for
          backendStub <- controllerBackendStubZIO(_.createUser :: Nil)
          response    <- backendStub.post[UserResponse]("user", request)
        yield assert(response)(isRight(equalTo(UserResponse(testUser.email))))
      },
      test("POST /user/login") {
        val request = LoginRequest(testUser.email, testUser.hashedPassword)
        for
          backendStub <- controllerBackendStubZIO(_.login :: Nil)
          response    <- backendStub.post[UserToken]("/user/login", request)
        yield assert(response)(
          isRight(equalTo(UserToken(testUser.email, "test-token", 3600L)))
        )
      },
      test("POST /user/login with wrong credentials") {
        val request = LoginRequest("foo@bar.com", "foobar")
        for
          backendStub <- controllerBackendStubZIO(_.login :: Nil)
          response    <- backendStub.post[UserToken]("/user/login", request)
        yield assert(response)(isLeft(equalTo("failed to login")))
      },
      test("PUT /user/password") {
        val request = UpdatePasswordRequest(testUser.email, testUser.hashedPassword, "new-password")
        for
          backendStub <- controllerBackendStubZIO(_.updatePassword :: Nil)
          response    <- backendStub.putAuth[UserResponse]("/user/password", request, testAuthToken)
        yield assert(response)(isRight(equalTo(UserResponse(testUser.email))))
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
          response <- backendStub.putAuth[UserResponse]("/user/password", request, "invalid-token")
        yield assert(response)(isLeft(equalTo("invalid token")))
      }
    ).provide(userServiceStub, jwtServiceStub)
