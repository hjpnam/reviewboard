package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.domain.data.Company
import com.hjpnam.reviewboard.http.request.CreateCompanyRequest
import com.hjpnam.reviewboard.service.CompanyService
import com.hjpnam.reviewboard.syntax.*
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*

object CompanyControllerSpec extends ZIOSpecDefault:
  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val testCompany = Company(1, "foo", "foo", "foo.com")
  private val serviceStub = new CompanyService {
    override def create(createRequest: CreateCompanyRequest): Task[Company] =
      ZIO.succeed(testCompany)

    override def getById(id: Long): Task[Option[Company]] =
      ZIO.succeed(Option.when(id == 1)(testCompany))

    override def getAll: Task[List[Company]] = ZIO.succeed(testCompany :: Nil)

    override def getBySlug(slug: String): Task[Option[Company]] =
      ZIO.succeed(Option.when(slug == testCompany.slug)(testCompany))
  }

  private def backendStubZIO(endpointFn: CompanyController => List[ServerEndpoint[Any, Task]]) = for
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
            .contains(testCompany),
          "inspect response from POST /companies"
        )
      },
      test("GET /companies with data") {
        val program = for
          backendStub <- backendStubZIO(controller => controller.getAll :: controller.create :: Nil)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[List[Company]].toOption)
            .contains(testCompany :: Nil),
          "inspect response from GET /companies"
        )
      },
      test("GET /companies/:id for non-existant company") {
        val program = for
          backendStub <- backendStubZIO(_.getById :: Nil)
          response <- basicRequest
            .get(uri"/companies/0")
            .send(backendStub)
        yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[Company].toOption)
            .isEmpty,
          "inspect response from GET /companies"
        )
      },
      test("GET /companies/:id for existing company") {
        val program = for
          backendStub <- backendStubZIO(controller =>
            controller.getById :: controller.create :: Nil
          )
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(testCompany),
          "inspect response from GET /companies"
        )
      }
    ).provide(ZLayer.succeed(serviceStub))
