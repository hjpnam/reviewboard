package com.hjpnam.reviewboard.service

import com.hjpnam.reviewboard.domain.data.Company
import com.hjpnam.reviewboard.http.request.CreateCompanyRequest
import com.hjpnam.reviewboard.repository.CompanyRepository
import com.hjpnam.reviewboard.syntax.*
import zio.*
import zio.test.*

import scala.collection.mutable

object CompanyServiceSpec extends ZIOSpecDefault:
  val service = ZIO.serviceWithZIO[CompanyService]

  val companyRepositoryStub = ZLayer.succeed(
    new CompanyRepository {
      val db = mutable.Map.empty[Long, Company]

      override def create(company: Company): Task[Company] = ZIO.succeed {
        val id         = db.keys.maxOption.getOrElse(0L) + 1
        val newCompany = company.copy(id = id)
        db += (id -> newCompany)
        newCompany
      }

      override def update(id: Long, op: Company => Company): Task[Company] = ZIO.attempt {
        val company = db(id)
        db += (id -> op(company))
        company
      }

      override def delete(id: Long): Task[Company] = ZIO.attempt {
        val company = db(id)
        db -= id
        company
      }

      override def getById(id: Long): Task[Option[Company]] = ZIO.succeed(db.get(id))

      override def getBySlug(slug: String): Task[Option[Company]] =
        ZIO.succeed(db.values.find(_.slug == slug))

      override def get: Task[List[Company]] = ZIO.succeed(db.values.toList)
    }
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyServiceTest")(
      test("create") {
        val companyZIO = service(_.create(CreateCompanyRequest("foo", "foo.com")))

        companyZIO.assert { company =>
          company.name == "foo" &&
          company.url == "foo.com" &&
          company.slug == "foo"
        }
      },
      test("getById") {
        val program = for
          company    <- service(_.create(CreateCompanyRequest("foo", "foo.com")))
          companyOpt <- service(_.getById(company.id))
        yield company -> companyOpt

        program.assert {
          case (company, Some(companyQueryRes)) =>
            company == companyQueryRes
          case _ => false
        }
      },
      test("getBySlug") {
        val program = for
          company    <- service(_.create(CreateCompanyRequest("foo", "foo.com")))
          companyOpt <- service(_.getBySlug(company.slug))
        yield company -> companyOpt

        program.assert {
          case (company, Some(companyQueryRes)) =>
            company == companyQueryRes
          case _ => false
        }
      },
      test("getAll") {
        val program = for
          company1  <- service(_.create(CreateCompanyRequest("foo", "foo.com")))
          company2  <- service(_.create(CreateCompanyRequest("bar", "bar.com")))
          companies <- service(_.getAll)
        yield (company1, company2, companies)

        program.map { case (company1, company2, companies) =>
          zio.test.assert(companies)(Assertion.hasSameElements(company1 :: company2 :: Nil))
        }
      }
    ).provide(CompanyService.live, companyRepositoryStub)
