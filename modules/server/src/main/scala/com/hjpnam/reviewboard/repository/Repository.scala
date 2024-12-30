package com.hjpnam.reviewboard.repository

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{TaskLayer, URLayer, ZLayer}

import javax.sql.DataSource

object Repository:
  def quillLayer: URLayer[DataSource, Quill.Postgres[SnakeCase.type]] =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  def dataSourceLayer: TaskLayer[DataSource] =
    Quill.DataSource.fromPrefix("app.db")

  val dataLayer: TaskLayer[Quill.Postgres[SnakeCase.type]] =
    dataSourceLayer >>> quillLayer
