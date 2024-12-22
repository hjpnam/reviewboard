package com.hjpnam.reviewboard

import com.hjpnam.reviewboard.http.HttpApi
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

  override def run = serverProgram.provide(Server.default)
