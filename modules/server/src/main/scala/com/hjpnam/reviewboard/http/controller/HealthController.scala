package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.http.endpoint.HealthEndpoint
import sttp.tapir.server.ServerEndpoint
import zio.{Task, UIO, ZIO}

class HealthController private extends BaseController with HealthEndpoint:
  val health = healthCheck.serverLogicSuccess[Task](_ => ZIO.succeed("all good"))
  override val routes: List[ServerEndpoint[Any, Task]] = health :: Nil

object HealthController:
  val makeZIO: UIO[HealthController] = ZIO.succeed(new HealthController)
