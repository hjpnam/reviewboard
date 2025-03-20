package com.hjpnam.reviewboard.page

import com.hjpnam.reviewboard.common.Constant
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom.Element

trait FormState:
  def errorList: List[Option[String]]
  def showStatus: Boolean
  def maybeSuccess: Option[String]

  def maybeError: Option[String] = errorList.find(_.isDefined).flatten.filter(_ => showStatus)
  def hasError: Boolean          = errorList.exists(_.isDefined)
  def maybeStatus: Option[Either[String, String]] =
    maybeError.map(Left(_)).orElse(maybeSuccess.map(Right(_))).filter(_ => showStatus)

abstract class FormPage[S <: FormState](title: String):
  def basicState: S
  val stateVar: Var[S] = Var(basicState)

  def apply() =
    div(
      onUnmountCallback(_ => stateVar.set(basicState)),
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
          div(cls := "top-section", h1(span(title))),
          children <-- stateVar.signal.map(_.maybeStatus).map(renderStatus).map(_.toList),
          form(
            nameAttr := "signin",
            cls      := "form",
            idAttr   := "form",
            renderChildren()
          )
        )
      )
    )

  def renderChildren(): List[ReactiveElement[Element]]

  def renderStatus(status: Option[Either[String, String]]) = status.map {
    case Left(error) =>
      div(
        cls := "page-status-errors",
        error
      )
    case Right(message) =>
      div(
        cls := "page-status-success",
        message
      )
  }

  def renderInput(
      name: String,
      uid: String,
      inputType: String,
      required: Boolean,
      plcholder: String,
      updateFn: (S, String) => S
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
