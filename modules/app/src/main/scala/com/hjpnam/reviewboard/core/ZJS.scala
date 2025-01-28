package com.hjpnam.reviewboard.core

import com.hjpnam.reviewboard.config.BackendClientConfig
import com.raquo.airstream.eventbus.EventBus
import sttp.client3.UriContext
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.{RIO, Runtime, Task, Unsafe, ZIO}

object ZJS:
  def backendCall = ZIO.serviceWithZIO[BackendClient]

  extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A])
    def emitTo(eventBus: EventBus[A]) =
      Unsafe.unsafely(
        Runtime.default.unsafe.fork(
          zio.tap(list => ZIO.attempt(eventBus.emit(list))).provide(BackendClient.configuredLive)
        )
      )

  extension [I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])
    def apply(payload: I): Task[O] =
      ZIO
        .serviceWithZIO[BackendClient](_.endpointRequestZIO(endpoint, payload))
        .provide(BackendClient.configuredLive)
