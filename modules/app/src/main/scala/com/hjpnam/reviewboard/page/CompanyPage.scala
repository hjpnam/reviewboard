package com.hjpnam.reviewboard.page

import com.hjpnam.reviewboard.common.Constant
import com.hjpnam.reviewboard.component.{Anchor, FilterPanel}
import com.hjpnam.reviewboard.core.BackendClient
import com.hjpnam.reviewboard.core.ZJS.*
import com.hjpnam.reviewboard.domain.data.Company
import com.raquo.laminar.api.L.{*, given}

object CompanyPage:
  val filterPanel = new FilterPanel

  val firstGetAll = EventBus[List[Company]]()
  val companyEvents: EventStream[List[Company]] =
    firstGetAll.events.mergeWith(
      filterPanel.appliedFilterStream.flatMapSwitch(companyFilter =>
        backendCall(_.company.searchEndpoint(companyFilter)).toEventStream
      )
    )

  def apply() =
    sectionTag(
      onMountCallback(_ => backendCall(_.company.getAllEndpoint(())).emitTo(firstGetAll)),
      cls := "section-1",
      div(
        cls := "container company-list-hero",
        h1(
          cls := "company-list-title",
          "Rock the JVM Companies Board"
        )
      ),
      div(
        cls := "container",
        div(
          cls := "row jvm-recent-companies-body",
          div(
            cls := "col-lg-4",
            filterPanel()
          ),
          div(
            cls := "col-lg-8",
            children <-- companyEvents.map(_.map(renderCompany))
          )
        )
      )
    )

  private def renderCompanyImg(company: Company) =
    img(
      cls := "img-fluid",
      src := company.image.getOrElse(Constant.companyLogoPlaceholder),
      alt := company.name
    )

  private def renderCompanyDetail(icon: String, value: String) =
    div(
      cls := "company-detail",
      i(cls := s"fa fa-$icon company-detail-icon"),
      p(
        cls := "company-detail-value",
        value
      )
    )

  private def fullLocationString(company: Company): String =
    (company.location, company.country) match
      case (Some(location), Some(country)) => s"$location, $country"
      case (Some(location), _)             => location
      case (_, Some(country))              => country
      case _                               => "N/A"

  private def renderCompanyOverview(company: Company) =
    div(
      cls := "company-summary",
      renderCompanyDetail("location-dot", fullLocationString(company)),
      renderCompanyDetail("tags", company.tags.mkString(", "))
    )

  private def renderAction(company: Company) =
    div(
      cls := "jvm-recent-companies-card-btn-apply",
      a(
        href   := company.url,
        target := "blank",
        button(
          `type` := "button",
          cls    := "btn btn-danger rock-action-btn",
          "Website"
        )
      )
    )

  private def renderCompany(company: Company) =
    div(
      cls := "jvm-recent-companies-cards",
      div(
        cls := "jvm-recent-companies-card-img",
        renderCompanyImg(company)
      ),
      div(
        cls := "jvm-recent-companies-card-contents",
        h5(
          Anchor.renderNavLink(
            company.name,
            s"/company/${company.id}",
            "company-title-link"
          )
        ),
        renderCompanyOverview(company)
      ),
      renderAction(company)
    )
