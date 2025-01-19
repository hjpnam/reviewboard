package com.hjpnam.reviewboard.util

import sttp.client3.*
import sttp.model.Method
import zio.Task
import zio.json.*

object RichSttpBackend:
  extension [RequestPayload: JsonCodec](backend: SttpBackend[Task, Nothing])
    def sendRequest[ResponsePayload: JsonCodec](
        method: Method,
        path: String,
        payload: RequestPayload,
        token: String = ""
    ): Task[Either[String, ResponsePayload]] =
      basicRequest
        .method(method, uri"$path")
        .body(payload.toJson)
        .auth
        .bearer(token)
        .send(backend)
        .map(_.body.flatMap(_.fromJson[ResponsePayload]))

    def post[ResponsePayload: JsonCodec](
        path: String,
        payload: RequestPayload
    ): Task[Either[String, ResponsePayload]] =
      sendRequest(Method.POST, path, payload)

    def postAuth[ResponsePayload: JsonCodec](
        path: String,
        payload: RequestPayload,
        token: String
    ): Task[Either[String, ResponsePayload]] =
      sendRequest(Method.POST, path, payload, token)

    def postNoResponse(path: String, payload: RequestPayload): Task[Unit] =
      basicRequest
        .method(Method.POST, uri"$path")
        .body(payload.toJson)
        .send(backend)
        .unit

    def put[ResponsePayload: JsonCodec](
        path: String,
        payload: RequestPayload
    ): Task[Either[String, ResponsePayload]] =
      sendRequest(Method.PUT, path, payload)

    def putAuth[ResponsePayload: JsonCodec](
        path: String,
        payload: RequestPayload,
        token: String
    ): Task[Either[String, ResponsePayload]] =
      sendRequest(Method.PUT, path, payload, token)

    def delete[ResponsePayload: JsonCodec](
        path: String,
        payload: RequestPayload
    ): Task[Either[String, ResponsePayload]] =
      sendRequest(Method.DELETE, path, payload)

    def deleteAuth[ResponsePayload: JsonCodec](
        path: String,
        payload: RequestPayload,
        token: String
    ): Task[Either[String, ResponsePayload]] =
      sendRequest(Method.DELETE, path, payload, token)

  extension (backend: SttpBackend[Task, Nothing])
    def get[ResponsePayload: JsonCodec](path: String): Task[Either[String, ResponsePayload]] =
      basicRequest
        .method(Method.GET, uri"$path")
        .send(backend)
        .map(_.body.flatMap(_.fromJson[ResponsePayload]))

    def getAuth[ResponsePayload: JsonCodec](
        path: String,
        token: String
    ): Task[Either[String, ResponsePayload]] =
      basicRequest
        .method(Method.GET, uri"$path")
        .auth
        .bearer(token)
        .send(backend)
        .map(_.body.flatMap(_.fromJson[ResponsePayload]))
