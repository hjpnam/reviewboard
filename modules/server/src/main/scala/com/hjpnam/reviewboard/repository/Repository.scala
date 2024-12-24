package com.hjpnam.reviewboard.repository

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill

object Repository:
  def quillLayer =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  def dataSourceLayer =
    Quill.DataSource.fromPrefix("app.db")

  val dataLayer =
    dataSourceLayer >>> quillLayer
