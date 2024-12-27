package com.hjpnam.reviewboard.repository

import com.hjpnam.reviewboard.domain.data.Company
import com.hjpnam.reviewboard.syntax.*
import com.hjpnam.reviewboard.util.Gen
import org.postgresql.ds.PGSimpleDataSource
import zio.*
import zio.test.*

import java.sql.SQLException

object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec with Gen:
  private val fooCompany = Company(1L, "foo", "foo", "foo.com")

  private def genCompany(): Company =
    Company(
      id = -1L,
      slug = genString(8),
      name = genString(8),
      url = genString(8)
    )

  override val initScriptPath = "sql/companies.sql"

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyRepositorySpec")(
      test("create a company") {
        val program = for
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(fooCompany)
        yield company

        program.assert {
          case Company(_, "foo", "foo", "foo.com", _, _, _, _, _) => true
          case _                                                  => false
        }
      },
      test("creating a duplicate should error") {
        val program = for
          repo <- ZIO.service[CompanyRepository]
          _    <- repo.create(fooCompany)
          err  <- repo.create(fooCompany).flip
        yield err

        program.assert(_.isInstanceOf[SQLException])
      },
      test("get by id and slug") {
        val program = for
          repo          <- ZIO.service[CompanyRepository]
          company       <- repo.create(fooCompany)
          fetchedById   <- repo.getById(company.id)
          fetchedBySlug <- repo.getBySlug(company.slug)
        yield (company, fetchedById, fetchedBySlug)

        program.assert {
          case (company, Some(fetchedById), Some(fetchedBySlug)) =>
            company == fetchedById && company == fetchedBySlug
          case _ => false
        }
      },
      test("update record") {
        val program = for
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(fooCompany)
          updated     <- repo.update(company.id, _.copy(url = "foo.org"))
          fetchedById <- repo.getById(company.id)
        yield updated -> fetchedById

        program.assert {
          case (updated, Some(fetchedById)) =>
            fetchedById == updated
          case _ => false
        }
      },
      test("delete record") {
        val program = for
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(fooCompany)
          _           <- repo.delete(company.id)
          fetchedById <- repo.getById(company.id)
        yield fetchedById

        program.assert(_.isEmpty)
      },
      test("get all records") {
        val program = for
          repo             <- ZIO.service[CompanyRepository]
          companies        <- ZIO.foreach(1 to 10)(_ => repo.create(genCompany()))
          companiesFetched <- repo.get
        yield companies -> companiesFetched

        program.map { case (companies, companiesFetched) =>
          zio.test.assert(companies)(Assertion.hasSameElements(companiesFetched))
        }
      }
    ).provide(CompanyRepository.live, dataSourceLayer, Repository.quillLayer, Scope.default)
