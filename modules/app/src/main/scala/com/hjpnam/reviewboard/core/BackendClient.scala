package com.hjpnam.reviewboard.core

import com.hjpnam.reviewboard.config.BackendClientConfig
import com.hjpnam.reviewboard.http.endpoint.{CompanyEndpoint, UserEndpoint}
import sttp.client3.UriContext
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.impl.zio.FetchZioBackend
import sttp.client3.{Request, SttpBackend}
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

trait BackendClient:
  val company: CompanyEndpoint
  val user: UserEndpoint
  def endpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any],
      payload: I
  ): Task[O]

object BackendClient:
  val live           = BackendClientLive.layer
  val configuredLive = BackendClientLive.configuredLayer

class BackendClientLive private (
    backend: SttpBackend[Task, ZioStreams & WebSockets],
    interpreter: SttpClientInterpreter,
    backendClientConfig: BackendClientConfig
) extends BackendClient:
  override val company: CompanyEndpoint = new CompanyEndpoint {}
  override val user: UserEndpoint       = new UserEndpoint {}

  private def endpointRequest[I, E, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  ): I => Request[Either[E, O], Any] =
    interpreter.toRequestThrowDecodeFailures(endpoint, backendClientConfig.uri)

  override def endpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any],
      payload: I
  ): Task[O] =
    backend.send(endpointRequest(endpoint)(payload)).map(_.body).absolve

object BackendClientLive:
  // need a type alias to appease implicit resolution of izumi tag
  // won't compile if alias is private for some reason.
  // https://github.com/softwaremill/sttp/issues/2368
  protected type ZioWebSocketsStreams = ZioStreams & WebSockets

  val layer: ZLayer[
    BackendClientConfig & SttpClientInterpreter & SttpBackend[Task, ZioWebSocketsStreams],
    Nothing,
    BackendClientLive
  ] = ZLayer {
    for
      backend     <- ZIO.service[SttpBackend[Task, ZioWebSocketsStreams]]
      interpreter <- ZIO.service[SttpClientInterpreter]
      config      <- ZIO.service[BackendClientConfig]
    yield new BackendClientLive(backend, interpreter, config)
  }

  val backend: SttpBackend[Task, ZioWebSocketsStreams] = FetchZioBackend()
  val configuredLayer: ULayer[BackendClientLive] =
    ZLayer.succeed(BackendClientConfig(Some(uri"http://localhost:8080")))
      ++ ZLayer.succeed(SttpClientInterpreter())
      ++ ZLayer.succeed(backend)
      >>> layer
