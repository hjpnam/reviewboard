package com.hjpnam.reviewboard

import com.hjpnam.reviewboard.http.controllers.HealthController
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault:

  val serverProgram =
    for
      controller <- HealthController.makeZIO
      _          <- Server.serve(ZioHttpInterpreter().toHttp(controller.health :: Nil))
      _          <- Console.printLine("server running")
    yield ()

  override def run = serverProgram.provide(Server.default)
