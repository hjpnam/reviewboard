package com.hjpnam.reviewboard.integration

import com.hjpnam.reviewboard.config.{JWTConfig, RecoveryTokenConfig}
import com.hjpnam.reviewboard.domain.data.UserToken
import com.hjpnam.reviewboard.http.controller.*
import com.hjpnam.reviewboard.http.request.*
import com.hjpnam.reviewboard.http.response.UserResponse
import com.hjpnam.reviewboard.repository.{RecoveryTokenRepository, Repository, RepositorySpec, UserRepository}
import com.hjpnam.reviewboard.service.*
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{HttpError as _, *}
import sttp.model.Method
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.json.*
import zio.test.*
import zio.test.Assertion.*
import zio.{Scope, Task, UIO, ZIO, ZLayer}

import scala.collection.mutable

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

  class EmailServiceProbe extends EmailService:
    val db = mutable.Map.empty[String, String]
    override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] =
      ZIO.succeed(db += (to -> token))
    override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ZIO.unit
    def probeToken(email: String): UIO[Option[String]] = ZIO.succeed(db.get(email))

  override def spec: Spec[TestEnvironment with Scope, Any] =
    import com.hjpnam.reviewboard.util.RichSttpBackend.*
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
        val newPassword = "new-password"
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
            UpdatePasswordRequest(testEmail, testPassword, newPassword),
            userToken.token
          )
          failedLogin <- backendStub.post[UserToken](
            "/user/login",
            LoginRequest(testEmail, testPassword)
          )
          successfulLogin <- backendStub.post[UserToken](
            "/user/login",
            LoginRequest(testEmail, newPassword)
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
      },
      test("recover password") {
        val newPassword = "newpassword"
        for
          backendStub <- backendStubZIO
          _ <- backendStub.post[UserResponse]("/user", RegisterUserRequest(testEmail, testPassword))
          _ <- backendStub.postNoResponse("/user/forgot", ForgotPasswordRequest(testEmail))
          emailService <- ZIO.service[EmailServiceProbe]
          token <- emailService
            .probeToken(testEmail)
            .someOrFail(new RuntimeException("expected token missing"))
          _ <- backendStub.postNoResponse(
            "/user/recover",
            RecoverPasswordRequest(testEmail, token, newPassword)
          )
          failedLogin <- backendStub.post[UserToken](
            "/user/login",
            LoginRequest(testEmail, testPassword)
          )
          successfulLogin <- backendStub.post[UserToken](
            "/user/login",
            LoginRequest(testEmail, newPassword)
          )
        yield assert(failedLogin)(
          isLeft(equalTo("failed to login"))
        ) && assert(successfulLogin)(
          isRight(hasField("email", _.email, equalTo(testEmail)))
        )
      }
    ).provide(
      UserService.live,
      JWTService.live,
      UserRepository.live,
      RecoveryTokenRepository.live,
      ZLayer.succeed(new EmailServiceProbe),
      Repository.quillLayer,
      dataSourceLayer,
      ZLayer.succeed(JWTConfig("secret", 3600L)),
      ZLayer.succeed(RecoveryTokenConfig(60000)),
      Scope.default
    )
