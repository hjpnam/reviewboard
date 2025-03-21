package com.hjpnam.reviewboard.http.request

import com.hjpnam.reviewboard.domain.data.Company
import io.github.arainko.ducktape.{into, Field}
import zio.json.JsonCodec

final case class CreateCompanyRequest(
    name: String,
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = Nil
) derives JsonCodec:
  def toCompany(id: Long): Company =
    this.into[Company].transform(Field.const(_.id, id), Field.const(_.slug, Company.makeSlug(name)))
