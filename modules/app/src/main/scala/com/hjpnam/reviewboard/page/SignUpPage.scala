package com.hjpnam.reviewboard.page

import com.hjpnam.reviewboard.common.Constant
import com.hjpnam.reviewboard.core.ZJS.*
import com.hjpnam.reviewboard.http.request.RegisterUserRequest
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom
import zio.ZIO

final case class SignUpFormState(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState:
  private val emailFormatError: Option[String] =
    Option.when(!email.matches(Constant.emailRegex))("Email is invalid")
  private val passwordEmptyError: Option[String] =
    Option.when(password.isEmpty)("Password can't be empty")
  private val confirmPasswordError: Option[String] =
    Option.when(password != confirmPassword)("Passwords not matching")

  override val errorList: List[Option[String]] =
    List(emailFormatError, passwordEmptyError, confirmPasswordError) :+ upstreamStatus.flatMap(
      _.left.toOption
    )
  override val maybeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)

object SignUpPage extends FormPage[SignUpFormState]("Sign Up"):
  override def basicState: SignUpFormState = SignUpFormState()
  override def renderChildren(): List[ReactiveElement[dom.Element]] = List(
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, email) => s.copy(email = email, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Password",
      "password-input",
      "password",
      true,
      "Your password",
      (s, pw) => s.copy(password = pw, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Confirm Password",
      "confirm-password-input",
      "password",
      true,
      "Confirm password",
      (s, pw) => s.copy(confirmPassword = pw, showStatus = false, upstreamStatus = None)
    ),
    button(
      `type` := "button",
      "Sign Up",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

  val submitter = Observer[SignUpFormState] { state =>
    if state.hasError then stateVar.update(_.copy(showStatus = true))
    else
      backendCall(_.user.createUserEndpoint(RegisterUserRequest(state.email, state.password)))
        .map(userResponse =>
          stateVar.update(
            _.copy(showStatus = true, upstreamStatus = Some(Right("Account created")))
          )
        )
        .tapError(e =>
          ZIO
            .succeed(
              stateVar.update(_.copy(showStatus = true, upstreamStatus = Some(Left(e.getMessage))))
            )
        )
        .runJs
  }
