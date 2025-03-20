package com.hjpnam.reviewboard.page

import com.hjpnam.reviewboard.core.Session
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom

final case class LogoutPageState() extends FormState:
  override val errorList: List[Option[String]] = Nil
  override val maybeSuccess: Option[String]    = None
  override val showStatus: Boolean             = false

object LogoutPage extends FormPage[LogoutPageState]("Log Out"):
  override def basicState: LogoutPageState = LogoutPageState()

  override def renderChildren(): List[ReactiveElement[dom.Element]] = List(
    div(
      onMountCallback(_ => Session.clearUserState()),
      cls := "centered-text",
      "Logged out"
    )
  )
