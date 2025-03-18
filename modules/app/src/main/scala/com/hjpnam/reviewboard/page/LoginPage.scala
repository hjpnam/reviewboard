package com.hjpnam.reviewboard.page

import com.hjpnam.reviewboard.common.Constant
import com.hjpnam.reviewboard.core.Session
import com.hjpnam.reviewboard.core.ZJS.*
import com.hjpnam.reviewboard.http.request.LoginRequest
import com.raquo.laminar.api.L.{*, given}
import frontroute.BrowserNavigation
import zio.ZIO

final case class LoginFormState(
    email: String = "",
    password: String = "",
    upstreamError: Option[String] = None,
    override val showStatus: Boolean = false
) extends FormState:
  private val emailFormatError: Option[String] =
    Option.when(!email.matches(Constant.emailRegex))("Email is invalid")
  private val passwordEmptyError: Option[String] =
    Option.when(password.isEmpty)("Password can't be empty")

  override val errorList: List[Option[String]] =
    List(emailFormatError, passwordEmptyError, upstreamError)
  // irrelevant to login page because page redirected after log in
  override val maybeSuccess: Option[String] = None

object LoginPage extends FormPage[LoginFormState]("Log In"):
  override val stateVar = Var(LoginFormState())
  val submitter = Observer[LoginFormState] { state =>
    if state.hasError then stateVar.update(_.copy(showStatus = true))
    else
      backendCall(_.user.generateTokenEndpoint(LoginRequest(state.email, state.password)))
        .map(userToken =>
          Session.setUserState(userToken)
          stateVar.set(LoginFormState())
          BrowserNavigation.replaceState("/")
        )
        .tapError(e =>
          ZIO
            .succeed(
              stateVar.update(_.copy(showStatus = true, upstreamError = Some(e.getMessage)))
            )
        )
        .runJs
  }

  def renderChildren() = List(
    renderInput(
      "Email",
      "form-id-1-todo",
      "text",
      true,
      "Your email",
      (s, email) => s.copy(email = email, showStatus = false, upstreamError = None)
    ),
    // an input of type password
    renderInput(
      "Password",
      "form-id-2-todo",
      "password",
      true,
      "Your password",
      (s, pw) => s.copy(password = pw, showStatus = false, upstreamError = None)
    ),
    button(
      `type` := "button",
      "Log In",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )
