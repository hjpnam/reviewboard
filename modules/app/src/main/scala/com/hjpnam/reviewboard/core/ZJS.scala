package com.hjpnam.reviewboard.core

import com.raquo.airstream.eventbus.EventBus
import sttp.tapir.Endpoint
import zio.{Runtime, Task, Unsafe, ZIO}

object ZJS:
  def backendCall = ZIO.serviceWithZIO[BackendClient]

  extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A])
    def emitTo(eventBus: EventBus[A]) =
      Unsafe.unsafely(
        Runtime.default.unsafe.fork(
          zio.tap(list => ZIO.attempt(eventBus.emit(list))).provide(BackendClient.configuredLive)
        )
      )

    def runJs = Unsafe.unsafely(Runtime.default.unsafe.runToFuture(zio.provide(BackendClient.configuredLive)))

  extension [I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])
    def apply(payload: I): Task[O] =
      ZIO
        .serviceWithZIO[BackendClient](_.endpointRequestZIO(endpoint, payload))
        .provide(BackendClient.configuredLive)
