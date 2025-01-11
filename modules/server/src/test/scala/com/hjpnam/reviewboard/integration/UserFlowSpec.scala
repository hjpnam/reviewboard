package com.hjpnam.reviewboard.integration

import com.hjpnam.reviewboard.config.JWTConfig
import com.hjpnam.reviewboard.domain.data.UserToken
import com.hjpnam.reviewboard.http.controller.*
import com.hjpnam.reviewboard.http.request.{
  DeleteUserRequest,
  LoginRequest,
  RegisterUserRequest,
  UpdatePasswordRequest
}
import com.hjpnam.reviewboard.http.response.UserResponse
import com.hjpnam.reviewboard.repository.{Repository, RepositorySpec, UserRepository}
import com.hjpnam.reviewboard.service.*
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{HttpError as _, *}
import sttp.model.Method
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.json.*
import zio.test.*
import zio.test.Assertion.*
import zio.{Scope, Task, ZIO, ZLayer}

object UserFlowSpec extends ZIOSpecDefault, RepositorySpec:
  override val initScriptPath: String = "sql/integration.sql"

  def backendStubZIO = for
    controller <- UserController.makeZIO
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(new RIOMonadError[Any]))
        .whenServerEndpointsRunLogic(controller.routes)
        .backend()
    )
  yield backendStub

  extension [RequestPayload: JsonCodec](backend: SttpBackend[Task, Nothing])
    def sendRequest[ResponsePayload: JsonCodec](
        method: Method,
        path: String,
        payload: RequestPayload,
        token: String = ""
    ): Task[Either[String, ResponsePayload]] =
      basicRequest
        .method(method, uri"$path")
        .body(payload.toJson)
        .auth
        .bearer(token)
        .send(backend)
        .map(_.body.flatMap(_.fromJson[ResponsePayload]))

    def post[ResponsePayload: JsonCodec](
        path: String,
        payload: RequestPayload
    ): Task[Either[String, ResponsePayload]] =
      sendRequest(Method.POST, path, payload)

    def put[ResponsePayload: JsonCodec](
        path: String,
        payload: RequestPayload
    ): Task[Either[String, ResponsePayload]] =
      sendRequest(Method.PUT, path, payload)

    def putAuth[ResponsePayload: JsonCodec](
        path: String,
        payload: RequestPayload,
        token: String
    ): Task[Either[String, ResponsePayload]] =
      sendRequest(Method.PUT, path, payload, token)

    def delete[ResponsePayload: JsonCodec](
        path: String,
        payload: RequestPayload
    ): Task[Either[String, ResponsePayload]] =
      sendRequest(Method.DELETE, path, payload)

    def deleteAuth[ResponsePayload: JsonCodec](
        path: String,
        payload: RequestPayload,
        token: String
    ): Task[Either[String, ResponsePayload]] =
      sendRequest(Method.DELETE, path, payload, token)

  override def spec: Spec[TestEnvironment with Scope, Any] =
    val testEmail    = "test@example.com"
    val testPassword = "test-password"
    suite("UserFlowSpec")(
      test("create user") {
        for
          backendStub <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse](
            "/user",
            RegisterUserRequest(testEmail, testPassword)
          )
          userRepo  <- ZIO.service[UserRepository]
          maybeUser <- userRepo.getByEmail(testEmail)
        yield assert(maybeResponse)(isRight(equalTo(UserResponse(testEmail))))
          && assert(maybeUser)(isSome(hasField("email", _.email, equalTo(testEmail))))
      },
      test("create and log in") {
        for
          backendStub <- backendStubZIO
          _ <- basicRequest
            .post(uri"/user")
            .body(RegisterUserRequest(testEmail, testPassword).toJson)
            .send(backendStub)
          maybeToken <- backendStub.post[UserToken](
            "/user/login",
            LoginRequest(testEmail, testPassword)
          )
        yield assert(maybeToken)(isRight(hasField("email", _.email, equalTo(testEmail))))
      },
      test("change password") {
        val newEmail = "new-password"
        for
          backendStub <- backendStubZIO
          _ <- basicRequest
            .post(uri"/user")
            .body(RegisterUserRequest(testEmail, testPassword).toJson)
            .send(backendStub)
          userToken <- backendStub
            .post[UserToken](
              "/user/login",
              LoginRequest(testEmail, testPassword)
            )
            .absolve
          _ <- backendStub.putAuth[UserResponse](
            "/user/password",
            UpdatePasswordRequest(testEmail, testPassword, newEmail),
            userToken.token
          )
          failedLogin <- backendStub.post[UserToken](
            "/user/login",
            LoginRequest(testEmail, testPassword)
          )
          successfulLogin <- backendStub.post[UserToken](
            "/user/login",
            LoginRequest(testEmail, newEmail)
          )
        yield assert(failedLogin)(
          isLeft(equalTo("failed to login"))
        ) && assert(successfulLogin)(
          isRight(hasField("email", _.email, equalTo(testEmail)))
        )
      },
      test("delete user") {
        for
          backendStub <- backendStubZIO
          _ <- basicRequest
            .post(uri"/user")
            .body(RegisterUserRequest(testEmail, testPassword).toJson)
            .send(backendStub)
          maybeToken <- backendStub
            .post[UserToken](
              "/user/login",
              LoginRequest(testEmail, testPassword)
            )
            .absolve
          _ <- backendStub.deleteAuth[UserResponse](
            "/user",
            DeleteUserRequest(testEmail, testPassword),
            maybeToken.token
          )
          userRepo  <- ZIO.service[UserRepository]
          maybeUser <- userRepo.getByEmail(testEmail)
        yield assert(maybeUser)(isNone)
      }
    ).provide(
      UserService.live,
      JWTService.live,
      UserRepository.live,
      Repository.quillLayer,
      dataSourceLayer,
      ZLayer.succeed(JWTConfig("secret", 3600L)),
      Scope.default
    )
