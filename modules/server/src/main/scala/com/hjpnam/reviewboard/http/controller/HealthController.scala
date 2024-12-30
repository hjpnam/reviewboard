package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.http.endpoint.HealthEndpoint
import sttp.tapir.ztapir.{ZServerEndpoint, given}
import zio.{UIO, ZIO}

class HealthController private extends BaseController with HealthEndpoint:
  val health = healthCheck.zServerLogic[Any](_ => ZIO.succeed("all good"))
  override val routes: List[ZServerEndpoint[Any, Any]] = health :: Nil

object HealthController:
  val makeZIO: UIO[HealthController] = ZIO.succeed(new HealthController)
