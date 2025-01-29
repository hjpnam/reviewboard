package com.hjpnam.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class Company(
    id: Long,
    slug: String,
    name: String,
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = Nil
) derives JsonCodec

object Company:
  def makeSlug(name: String): String =
    name
      .replaceAll(" +", " ")
      .split(" ")
      .map(_.toLowerCase())
      .mkString("-")
