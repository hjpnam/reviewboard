package com.hjpnam.reviewboard

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object App:
  def main(args: Array[String]): Unit =
    val containerNode = dom.document.querySelector("#app")
    render(containerNode, div("from main"))
