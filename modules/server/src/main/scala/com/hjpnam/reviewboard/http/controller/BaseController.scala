package com.hjpnam.reviewboard.http.controller

import sttp.tapir.ztapir.ZServerEndpoint
import zio.Task

trait BaseController:
  val routes: List[ZServerEndpoint[Any, Any]]
