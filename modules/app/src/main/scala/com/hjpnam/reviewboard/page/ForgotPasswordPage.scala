package com.hjpnam.reviewboard.page

import com.hjpnam.reviewboard.common.Constant
import com.hjpnam.reviewboard.component.Anchor
import com.hjpnam.reviewboard.core.ZJS.*
import com.hjpnam.reviewboard.http.request.ForgotPasswordRequest
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom.Element
import zio.ZIO

final case class ForgotPasswordState(
    email: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState:
  override val errorList: List[Option[String]] = List(
    Option.when(!email.matches(Constant.emailRegex))("Email is invalid.")
  ) :+ upstreamStatus.flatMap(_.left.toOption)

  override val maybeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)

object ForgotPasswordPage extends FormPage[ForgotPasswordState]("Forgot Password"):
  override def basicState: ForgotPasswordState = ForgotPasswordState()

  override def renderChildren(): List[ReactiveElement[Element]] = List(
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, email) => s.copy(email = email, showStatus = false, upstreamStatus = None)
    ),
    button(
      `type` := "button",
      "Send Recovery Email",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    ),
    Anchor.renderNavLink("Have a recovery token?", "/recover", "auth-link")
  )

  val submitter = Observer[ForgotPasswordState] { state =>
    if state.hasError then stateVar.update(_.copy(showStatus = true))
    else
      backendCall(_.user.forgotPasswordEndpoint(ForgotPasswordRequest(state.email)))
        .as {
          stateVar.update(
            _.copy(showStatus = true, upstreamStatus = Some(Right("Check your email")))
          )
        }
        .tapError(e =>
          ZIO
            .succeed(
              stateVar.update(_.copy(showStatus = true, upstreamStatus = Some(Left(e.getMessage))))
            )
        )
        .runJs
  }
