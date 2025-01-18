package com.hjpnam.reviewboard

import com.hjpnam.reviewboard.http.HttpApi
import com.hjpnam.reviewboard.repository.*
import com.hjpnam.reviewboard.service.*
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
      // service
      CompanyService.live,
      ReviewService.live,
      UserService.live,
      JWTService.configuredLive,
      EmailService.configuredLive,
      // repository
      CompanyRepository.live,
      ReviewRepository.live,
      UserRepository.live,
      RecoveryTokenRepository.configuredLive,
      Repository.dataLayer
    )
