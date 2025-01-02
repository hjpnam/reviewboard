package com.hjpnam.reviewboard

import com.hjpnam.reviewboard.config.{Configs, JWTConfig}
import com.hjpnam.reviewboard.http.HttpApi
import com.hjpnam.reviewboard.repository.{
  CompanyRepository,
  CompanyRepositoryLive,
  Repository,
  ReviewRepository,
  UserRepository
}
import com.hjpnam.reviewboard.service.{CompanyService, JWTService, ReviewService, UserService}
import io.getquill.SnakeCase
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.*
import zio.http.Server

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
      // config
      Configs.makeLayer[JWTConfig]("app.jwt"),
      // service
      CompanyService.live,
      ReviewService.live,
      UserService.live,
      JWTService.live,
      // repository
      CompanyRepository.live,
      ReviewRepository.live,
      UserRepository.live,
      Repository.dataLayer
    )
