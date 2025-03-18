package com.hjpnam.reviewboard.page

import com.hjpnam.reviewboard.common.Constant
import com.hjpnam.reviewboard.core.Session
import com.raquo.laminar.api.L.{*, given}
import com.hjpnam.reviewboard.core.ZJS.*
import com.hjpnam.reviewboard.http.request.LoginRequest
import frontroute.BrowserNavigation
import zio.ZIO

object LoginPage:
  final case class State(
      email: String = "",
      password: String = "",
      showStatus: Boolean = false,
      upstreamError: Option[String] = None
  ):
    val emailFormatError: Option[String] =
      Option.when(!email.matches(Constant.emailRegex))("Email is invalid")

    val passwordEmptyError: Option[String] =
      Option.when(password.isEmpty)("Password can't be empty")

    val errorList: List[Option[String]] = List(emailFormatError, passwordEmptyError, upstreamError)
    val maybeError: Option[String] = errorList.find(_.isDefined).flatten.filter(_ => showStatus)
    val hasError: Boolean          = errorList.exists(_.isDefined)

  val stateVar = Var(State())
  val submitter = Observer[State] { state =>
    if state.hasError then stateVar.update(_.copy(showStatus = true))
    else
      backendCall(_.user.generateTokenEndpoint(LoginRequest(state.email, state.password)))
        .map(userToken =>
          Session.setUserState(userToken)
          stateVar.set(State())
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

  def apply() =
    div(
      cls := "row",
      div(
        cls := "col-md-5 p-0",
        div(
          cls := "logo",
          img(
            src := Constant.logoImage,
            alt := "Rock the JVM"
          )
        )
      ),
      div(
        cls := "col-md-7",
        // right
        div(
          cls := "form-section",
          div(cls := "top-section", h1(span("Log In"))),
          children <-- stateVar.signal.map(_.maybeError).map(_.map(renderError)).map(_.toList),
          maybeRenderSuccess(),
          form(
            nameAttr := "signin",
            cls      := "form",
            idAttr   := "form",
            // an input of type text
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
        )
      )
    )

  def maybeRenderSuccess(shouldShow: Boolean = false) =
    if shouldShow then
      div(
        cls := "page-status-success",
        "This is a success"
      )
    else div()

  def renderError(error: String) =
    div(
      cls := "page-status-errors",
      error
    )

  def renderInput(
      name: String,
      uid: String,
      inputType: String,
      required: Boolean,
      plcholder: String,
      updateFn: (State, String) => State
  ) =
    div(
      cls := "row",
      div(
        cls := "col-md-12",
        div(
          cls := "form-input",
          label(
            forId := uid,
            cls   := "form-label",
            if required then span("*") else span(),
            name
          ),
          input(
            `type`      := inputType,
            cls         := "form-control",
            idAttr      := uid,
            placeholder := plcholder,
            onInput.mapToValue --> stateVar.updater(updateFn)
          )
        )
      )
    )
