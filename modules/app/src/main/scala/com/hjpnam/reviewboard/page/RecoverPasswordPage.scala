package com.hjpnam.reviewboard.page

import com.hjpnam.reviewboard.common.Constant
import com.hjpnam.reviewboard.core.ZJS.*
import com.hjpnam.reviewboard.http.request.RecoverPasswordRequest
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom.Element
import zio.ZIO

final case class RecoverPasswordState(
    email: String = "",
    token: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState:
  override val errorList: List[Option[String]] = List(
    Option.when(!email.matches(Constant.emailRegex))("Email is invalid."),
    Option.when(token.isEmpty)("Please provide a token."),
    Option.when(newPassword.isEmpty)("Password can't be empty."),
    Option.when(newPassword != confirmPassword)("Passwords must match.")
  ) :+ upstreamStatus.flatMap(_.left.toOption)

  override val maybeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)

object RecoverPasswordPage extends FormPage[RecoverPasswordState]("Recover Password"):

  override def basicState: RecoverPasswordState = RecoverPasswordState()

  override def renderChildren(): List[ReactiveElement[Element]] = List(
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, email) => s.copy(email = email, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Token",
      "token-input",
      "text",
      true,
      "Token",
      (s, token) => s.copy(token = token, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "New Password",
      "password-input",
      "password",
      true,
      "New password",
      (s, pw) => s.copy(newPassword = pw, showStatus = false, upstreamStatus = None)
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
      "Update Password",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

  val submitter = Observer[RecoverPasswordState] { state =>
    if state.hasError then stateVar.update(_.copy(showStatus = true))
    else
      backendCall(
        _.user.recoverPasswordEndpoint(
          RecoverPasswordRequest(state.email, state.token, state.newPassword)
        )
      )
        .map(userResponse =>
          stateVar.update(
            _.copy(showStatus = true, upstreamStatus = Some(Right("Password updated")))
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
