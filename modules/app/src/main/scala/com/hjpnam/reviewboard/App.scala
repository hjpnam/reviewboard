package com.hjpnam.reviewboard

import com.hjpnam.reviewboard.component.{Header, Router}
import com.hjpnam.reviewboard.core.Session
import com.raquo.laminar.api.L.{*, given}
import frontroute.LinkHandler
import org.scalajs.dom

object App:
  val app = div(
    onMountCallback(_ => Session.loadUserState()),
    Header(),
    Router()
  ).amend(LinkHandler.bind)

  def main(args: Array[String]): Unit =
    val containerNode = dom.document.querySelector("#app")
    render(containerNode, app)
