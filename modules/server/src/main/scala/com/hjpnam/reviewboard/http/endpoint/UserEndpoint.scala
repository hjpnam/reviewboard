package com.hjpnam.reviewboard.http.endpoint

import com.hjpnam.reviewboard.domain.data.UserToken
import com.hjpnam.reviewboard.http.request.{
  DeleteUserRequest,
  LoginRequest,
  RegisterUserRequest,
  UpdatePasswordRequest
}
import com.hjpnam.reviewboard.http.response.UserResponse
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*

trait UserEndpoint extends BaseEndpoint, SecureBaseEndpoint:
  val createUserEndpoint =
    baseEndpoint
      .tag("user")
      .name("register")
      .description("Register a new user with email and password.")
      .in("user")
      .post
      .in(jsonBody[RegisterUserRequest])
      .out(jsonBody[UserResponse])

  val updatePasswordEndpoint =
    secureBaseEndpoint
      .tag("user")
      .name("update password")
      .description("Update a user's password.")
      .in("user" / "password")
      .put
      .in(jsonBody[UpdatePasswordRequest])
      .out(jsonBody[UserResponse])

  val deleteUserEndpoint =
    secureBaseEndpoint
      .tag("user")
      .name("delete")
      .description("Delete user account.")
      .in("user")
      .delete
      .in(jsonBody[DeleteUserRequest])
      .out(jsonBody[UserResponse])

  val generateTokenEndpoint =
    baseEndpoint
      .tag("user")
      .name("login user")
      .description("Login and generate a JWT token.")
      .in("user" / "login")
      .post
      .in(jsonBody[LoginRequest])
      .out(jsonBody[UserToken])
