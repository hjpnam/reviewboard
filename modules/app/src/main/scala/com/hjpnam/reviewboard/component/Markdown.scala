package com.hjpnam.reviewboard.component

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("showdown", JSImport.Default)
object MarkdownLib extends js.Object:
  @js.native
  class Converter extends js.Object:
    def makeHtml(text: String): String              = js.native
    def setOption(key: String, value: String): Unit = js.native

object Markdown:
  def toHtml(text: String): String =
    val converter = new MarkdownLib.Converter()
    converter.setOption("strikethrough", "true")
    converter.makeHtml(text)
