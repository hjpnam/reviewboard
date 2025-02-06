package com.hjpnam.reviewboard.core

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import sttp.tapir.Endpoint
import zio.{CancelableFuture, Runtime, Task, Unsafe, ZIO}

object ZJS:
  def backendCall = ZIO.serviceWithZIO[BackendClient]

  extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A])
    def emitTo(eventBus: EventBus[A]) =
      Unsafe.unsafely(
        Runtime.default.unsafe.fork(
          zio.tap(list => ZIO.attempt(eventBus.emit(list))).provide(BackendClient.configuredLive)
        )
      )

    def toEventStream: EventStream[A] =
      val bus = EventBus[A]()
      emitTo(bus)
      bus.events

    def runJs: CancelableFuture[A] =
      Unsafe.unsafely(Runtime.default.unsafe.runToFuture(zio.provide(BackendClient.configuredLive)))

  extension [I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])
    def apply(payload: I): Task[O] =
      ZIO
        .serviceWithZIO[BackendClient](_.endpointRequestZIO(endpoint, payload))
        .provide(BackendClient.configuredLive)
