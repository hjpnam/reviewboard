package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.domain.error.Unauthorized
import com.hjpnam.reviewboard.http.endpoint.{SecureBaseEndpoint, UserEndpoint}
import com.hjpnam.reviewboard.http.request.RecoverPasswordRequest
import com.hjpnam.reviewboard.http.response.UserResponse
import com.hjpnam.reviewboard.http.syntax.*
import com.hjpnam.reviewboard.service.{JWTService, UserService}
import sttp.tapir.ztapir.*
import zio.ZIO

class UserController private (userService: UserService, jwtService: JWTService)
    extends BaseController,
      UserEndpoint,
      SecureBaseEndpoint(jwtService):
  val createUser = createUserEndpoint.zServerLogic[Any](req =>
    userService
      .registerUser(req.email, req.password)
      .map(user => UserResponse(user.email))
      .mapToHttpError
  )

  val login = generateTokenEndpoint.zServerLogic[Any](req =>
    userService
      .generateToken(req.email, req.password)
      .someOrFail(Unauthorized("failed to login"))
      .mapToHttpError
  )

  val updatePassword = updatePasswordEndpoint
    .serverLogic[Any] { _ => req =>
      userService
        .updatePassword(req.email, req.oldPassword, req.newPassword)
        .map(user => UserResponse(user.email))
        .mapToHttpError
    }

  val deleteUser = deleteUserEndpoint
    .serverLogic[Any] { _ => req =>
      userService
        .deleteUser(req.email, req.password)
        .map(user => UserResponse(user.email))
        .mapToHttpError
    }

  val forgotPassword = forgotPasswordEndpoint
    .zServerLogic[Any] { req => userService.sendPasswordRecoveryToken(req.email).mapToHttpError }

  val recoverPassword = recoverPasswordEndpoint
    .zServerLogic[Any] { case RecoverPasswordRequest(email, token, newPassword) =>
      userService
        .recoverPassword(email, token, newPassword)
        .filterOrFail(identity)(Unauthorized(""))
        .unit
        .mapToHttpError
    }

  override val routes: List[ZServerEndpoint[Any, Any]] =
    createUser :: login :: updatePassword :: deleteUser :: forgotPassword :: recoverPassword :: Nil

object UserController:
  val makeZIO: ZIO[JWTService & UserService, Nothing, UserController] =
    for
      userService <- ZIO.service[UserService]
      jwtService  <- ZIO.service[JWTService]
    yield new UserController(userService, jwtService)
