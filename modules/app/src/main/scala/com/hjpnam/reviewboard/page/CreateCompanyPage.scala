package com.hjpnam.reviewboard.page

import com.hjpnam.reviewboard.common.Constant
import com.hjpnam.reviewboard.core.ZJS.*
import com.hjpnam.reviewboard.http.request.CreateCompanyRequest
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
import org.scalajs.dom.{
  document,
  CanvasRenderingContext2D,
  Element,
  File,
  FileReader,
  HTMLCanvasElement,
  HTMLImageElement
}
import zio.ZIO

final case class CreateCompanyState(
    name: String = "",
    url: String = "",
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = Nil,
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState:
  override val errorList: List[Option[String]] = List(
    Option.when(name.isEmpty)("The name can't be empty."),
    Option.when(!url.matches(Constant.urlRegex))("URL is invalid.")
  ) :+ upstreamStatus.flatMap(_.left.toOption)

  override val maybeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)

  val toRequest: CreateCompanyRequest = CreateCompanyRequest(
    name,
    url,
    location,
    country,
    industry,
    image,
    tags
  )

object CreateCompanyPage extends FormPage[CreateCompanyState]("Create Company"):
  override def basicState: CreateCompanyState = CreateCompanyState()

  override def renderChildren(): List[ReactiveElement[Element]] = List(
    renderInput(
      "Company Name",
      "company-name-input",
      "text",
      true,
      "ACME Inc.",
      (state, name) => state.copy(name = name, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Company URL",
      "company-url-input",
      "text",
      true,
      "https://acme.com",
      (state, url) => state.copy(url = url, showStatus = false, upstreamStatus = None)
    ),
    renderLogoUpload(
      "company-logo-input",
      "Company Logo",
      false
    ),
    renderInput(
      "Location",
      "company-location-input",
      "text",
      false,
      "Somewhere",
      (state, location) =>
        state.copy(location = Some(location), showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Country",
      "company-country-input",
      "text",
      false,
      "Arstozka",
      (state, country) => state.copy(country = Some(country), showStatus = false)
    ),
    renderInput(
      "Industry",
      "company-industry-input",
      "text",
      false,
      "Industry",
      (state, industry) => state.copy(industry = Some(industry), showStatus = false)
    ),
    renderInput(
      "Tags - separate by ','",
      "company-tags-input",
      "text",
      false,
      "Tag1, Tag2",
      (state, tags) => state.copy(tags = tags.split(",").map(_.trim).toList, showStatus = false)
    ),
    button(
      `type` := "button",
      "Create Company",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

  val submitter = Observer[CreateCompanyState] { state =>
    if state.hasError then stateVar.update(_.copy(showStatus = true))
    else
      backendCall(_.company.createEndpoint(state.toRequest))
        .map(company =>
          stateVar.update(
            _.copy(showStatus = true, upstreamStatus = Some(Right("Company created")))
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

  val fileUploader = (files: List[File]) => {
    val maybeFile = files.headOption.filter(_.size > 0)
    maybeFile.foreach(file =>
      val reader = new FileReader

      reader.onload = _ => {
        val fakeImg = document.createElement("img").asInstanceOf[HTMLImageElement]
        fakeImg.addEventListener(
          "load",
          _ => {
            val canvas          = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
            val context         = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
            val (width, height) = computeDimension(fakeImg.width, fakeImg.height)
            context.drawImage(fakeImg, 0, 0, width, height)
            stateVar.update(_.copy(image = Some(canvas.toDataURL(file.`type`))))
          }
        )

        fakeImg.src = reader.result.toString
      }

      reader.readAsDataURL(file)
    )
  }

  private def computeDimension(width: Int, height: Int): (Int, Int) = if width > height then
    val ratio     = width * 1.0 / 256
    val newWidth  = width / ratio
    val newHeight = height / ratio
    newWidth.toInt -> newHeight.toInt
  else
    val (newHeight, newWidth) = computeDimension(height, width)
    newWidth -> newHeight

  private def renderLogoUpload(uid: String, name: String, required: Boolean) =
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
            `type` := "file",
            cls    := "form-control",
            idAttr := uid,
            accept := "image/*",
            onChange.mapToFiles --> fileUploader
          )
        )
      )
    )
