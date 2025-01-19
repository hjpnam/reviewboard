package com.hjpnam.reviewboard.http.controller.util

import sttp.client3.SttpBackend
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*

trait BackendStub:
  def backendStubZIO[R, E, A](
      controllerZIO: ZIO[R, E, A]
  )(endpointFn: A => List[ServerEndpoint[Any, Task]]): ZIO[R, E, SttpBackend[Task, Nothing]] = for
    controller <- controllerZIO
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(new RIOMonadError[Any]))
        .whenServerEndpointsRunLogic(endpointFn(controller))
        .backend()
    )
  yield backendStub
