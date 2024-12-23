package com.hjpnam.reviewboard

import com.hjpnam.reviewboard.http.HttpApi
import com.hjpnam.reviewboard.repository.{CompanyRepository, CompanyRepositoryLive}
import com.hjpnam.reviewboard.service.CompanyService
import io.getquill.SnakeCase
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.*
import zio.http.Server
import io.getquill.jdbczio.Quill

object Application extends ZIOAppDefault:

  val serverProgram =
    for
      endpoints <- HttpApi.endpointsZIO
      _         <- Server.serve(ZioHttpInterpreter().toHttp(endpoints))
      _         <- Console.printLine("server running")
    yield ()

  override def run =
    serverProgram.provide(
      Server.default,
      CompanyService.live,
      CompanyRepository.live,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("app.db")
    )
