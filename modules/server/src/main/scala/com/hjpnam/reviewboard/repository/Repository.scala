package com.hjpnam.reviewboard.repository

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{RLayer, URLayer, ZLayer}

import javax.sql.DataSource

object Repository:
  def quillLayer: URLayer[DataSource, Quill.Postgres[SnakeCase.type]] =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  def dataSourceLayer: RLayer[Any, DataSource] =
    Quill.DataSource.fromPrefix("app.db")

  val dataLayer: RLayer[Any, Quill.Postgres[SnakeCase.type]] =
    dataSourceLayer >>> quillLayer
