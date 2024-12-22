package com.hjpnam.reviewboard.http.controllers

import com.hjpnam.reviewboard.http.endpoints.HealthEndpoint
import sttp.tapir.ztapir.*
import zio.{UIO, ZIO}

class HealthController private extends BaseController with HealthEndpoint:
  val health = healthCheck.zServerLogic[Any](_ => ZIO.succeed("all good"))
  override val routes: List[ZServerEndpoint[Any, Any]] = health :: Nil

object HealthController:
  val makeZIO: UIO[HealthController] = ZIO.succeed(new HealthController)
