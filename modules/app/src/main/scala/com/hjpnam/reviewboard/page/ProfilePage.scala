package com.hjpnam.reviewboard.page

import com.hjpnam.reviewboard.core.Session
import com.hjpnam.reviewboard.core.ZJS.*
import com.hjpnam.reviewboard.http.request.UpdatePasswordRequest
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom.Element
import zio.ZIO

final case class ChangePasswordState(
    password: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState:
  override val errorList: List[Option[String]] = List(
    Option.when(password.isEmpty)("Password must not be empty"),
    Option.when(newPassword.isEmpty)("New password must not be empty"),
    Option.when(newPassword != confirmPassword)(
      "New password does not match confirm password entry"
    )
  ) :+ upstreamStatus.flatMap(_.left.toOption)

  override val maybeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)

object ProfilePage extends FormPage[ChangePasswordState]("Profile"):
  override def basicState: ChangePasswordState = ChangePasswordState()
  override def renderChildren(): List[ReactiveElement[Element]] =
    if Session.isActive then
      List(
        renderInput(
          "Password",
          "password-input",
          "password",
          true,
          "Your password",
          (s, pw) => s.copy(password = pw, showStatus = false, upstreamStatus = None)
        ),
        renderInput(
          "New Password",
          "new-password-input",
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
          "Change Password",
          onClick.preventDefault.mapTo(stateVar.now()) --> submitter
        )
      )
    else
      List(
        div(
          cls := "centered-text",
          "You're not logged in."
        )
      )

  val submitter = Observer[ChangePasswordState] { state =>
    if state.hasError then stateVar.update(_.copy(showStatus = true))
    else
      backendCall(
        _.user.updatePasswordEndpoint(UpdatePasswordRequest(state.password, state.newPassword))
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
