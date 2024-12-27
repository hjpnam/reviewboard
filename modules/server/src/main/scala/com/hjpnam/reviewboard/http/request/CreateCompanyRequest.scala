package com.hjpnam.reviewboard.http.request

import com.hjpnam.reviewboard.domain.data.Company
import zio.json.{DeriveJsonCodec, JsonCodec}

final case class CreateCompanyRequest(
    name: String,
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = Nil
):
  def toCompany(id: Long): Company =
    Company(id, Company.makeSlug(name), name, url, location, country, industry, image, tags)

object CreateCompanyRequest:
  given codec: JsonCodec[CreateCompanyRequest] = DeriveJsonCodec.gen[CreateCompanyRequest]
