package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.domain.data.Company
import com.hjpnam.reviewboard.http.request.CreateCompanyRequest
import com.hjpnam.reviewboard.syntax.*
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.{RIOMonadError, ZServerEndpoint}
import zio.*
import zio.json.*
import zio.test.*

object CompanyControllerSpec extends ZIOSpecDefault:
  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private def backendStubZIO(endpointFn: CompanyController => List[ZServerEndpoint[Any, Any]]) = for
    controller <- CompanyController.makeZIO
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointsRunLogic(endpointFn(controller))
        .backend()
    )
  yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyControllerSpec")(
      test("POST /companies") {
        val program =
          for
            backendStub <- backendStubZIO(_.create :: Nil)
            response <- basicRequest
              .post(uri"/companies")
              .body(CreateCompanyRequest("foo", "foo.com").toJson)
              .send(backendStub)
          yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(Company(1, "foo", "foo", "foo.com")),
          "inspect response from POST /companies"
        )
      },
      test("GET /companies without data") {
        val program = for
          backendStub <- backendStubZIO(_.getAll :: Nil)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[List[Company]].toOption)
            .contains(Nil),
          "inspect response from POST /companies"
        )
      },
      test("GET /companies with data") {
        val program = for
          backendStub <- backendStubZIO(controller => controller.getAll :: controller.create :: Nil)
          _ <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("foo", "foo.com").toJson)
            .send(backendStub)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[List[Company]].toOption)
            .contains(Company(1, "foo", "foo", "foo.com") :: Nil),
          "inspect response from POST /companies"
        )
      },
      test("GET /companies/:id without data") {
        val program = for
          backendStub <- backendStubZIO(_.getById :: Nil)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[Company].toOption)
            .isEmpty,
          "inspect response from POST /companies"
        )
      },
      test("GET /companies/:id with data") {
        val program = for
          backendStub <- backendStubZIO(controller =>
            controller.getById :: controller.create :: Nil
          )
          _ <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("foo", "foo.com").toJson)
            .send(backendStub)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(Company(1, "foo", "foo", "foo.com")),
          "inspect response from POST /companies"
        )
      }
    )
