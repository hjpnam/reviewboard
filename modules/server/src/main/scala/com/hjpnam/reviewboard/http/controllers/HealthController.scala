package com.hjpnam.reviewboard.http.controllers

import com.hjpnam.reviewboard.http.endpoints.HealthEndpoint
import sttp.tapir.ztapir.given
import zio.{UIO, ZIO}

class HealthController private extends HealthEndpoint:
  val health = healthCheck.zServerLogic[Any](_ => ZIO.succeed("all good"))

object HealthController:
  val makeZIO: UIO[HealthController] = ZIO.succeed(new HealthController)
