package com.hjpnam.reviewboard.component

import com.hjpnam.reviewboard.common.Constant
import com.hjpnam.reviewboard.domain.data.Company
import com.raquo.laminar.api.L.{*, given}

object CompanyComponents:
  def renderCompanyImg(company: Company) =
    img(
      cls := "img-fluid",
      src := company.image.getOrElse(Constant.companyLogoPlaceholder),
      alt := company.name
    )

  def renderCompanyDetail(icon: String, value: String) =
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

  def renderCompanyOverview(company: Company) =
    div(
      cls := "company-summary",
      renderCompanyDetail("location-dot", fullLocationString(company)),
      renderCompanyDetail("tags", company.tags.mkString(", "))
    )
