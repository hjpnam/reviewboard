package com.hjpnam.reviewboard.repository

import com.hjpnam.reviewboard.domain.data.{Company, CompanyFilter}
import com.hjpnam.reviewboard.util.TestGen
import org.postgresql.ds.PGSimpleDataSource
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.sql.SQLException

object CompanyRepositorySpec extends ZIOSpecDefault, RepositorySpec, TestGen:
  private val fooCompany = Company(1L, "foo", "foo", "foo.com")

  private def testCompany(): Company =
    Company(
      id = -1L,
      slug = genString(8),
      name = genString(8),
      url = genString(8)
    )

  override val initScriptPath = "sql/company.sql"

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyRepositorySpec")(
      test("create a company") {
        for
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(fooCompany)
        yield assert(company)(hasField("slug", _.slug, equalTo("foo")))
          && assert(company)(hasField("name", _.name, equalTo("foo")))
          && assert(company)(hasField("url", _.url, equalTo("foo.com")))
      },
      test("creating a duplicate should error") {
        for
          repo <- ZIO.service[CompanyRepository]
          _    <- repo.create(fooCompany)
          err  <- repo.create(fooCompany).exit
        yield assert(err)(failsWithA[SQLException])
      },
      test("get by id and slug") {
        for
          repo          <- ZIO.service[CompanyRepository]
          company       <- repo.create(fooCompany)
          fetchedById   <- repo.getById(company.id)
          fetchedBySlug <- repo.getBySlug(company.slug)
        yield assert(fetchedById)(isSome(equalTo(company)))
          && assert(fetchedBySlug)(isSome(equalTo(company)))
      },
      test("update record") {
        for
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(fooCompany)
          updated     <- repo.update(company.id, _.copy(url = "foo.org"))
          fetchedById <- repo.getById(company.id)
        yield assert(fetchedById)(isSome(equalTo(updated)))
      },
      test("delete record") {
        for
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(fooCompany)
          _           <- repo.delete(company.id)
          fetchedById <- repo.getById(company.id)
        yield assert(fetchedById)(isNone)
      },
      test("get all records") {
        for
          repo             <- ZIO.service[CompanyRepository]
          companies        <- ZIO.foreach(1 to 10)(_ => repo.create(testCompany()))
          companiesFetched <- repo.get
        yield assert(companies)(hasSameElements(companiesFetched))
      },
      test("get all filters") {
        val company1 = testCompany().copy(
          industry = Some(genString(8)),
          location = Some(genString(8)),
          country = Some(genString(8)),
          tags = genString(8) :: genString(8) :: Nil
        )
        val company2 = company1.copy(
          slug = company1.slug.drop(1),
          name = company1.name.drop(1),
          url = company1.url.drop(1)
        )
        val company3 = testCompany().copy(
          industry = None,
          location = Some(genString(8)),
          country = Some(genString(8)),
          tags = Nil
        )
        val companies          = company1 :: company2 :: company3 :: Nil
        val expectedLocations  = companies.flatMap(_.location.toList)
        val expectedCountries  = companies.flatMap(_.country.toList)
        val expectedIndustries = companies.flatMap(_.industry.toList)
        val expectedTags       = companies.flatMap(_.tags)

        for
          repo    <- ZIO.service[CompanyRepository]
          _       <- ZIO.foreachDiscard(companies)(repo.create)
          filters <- repo.uniqueAttributes
        yield assert(filters)(
          hasField("locations", _.locations, hasSameElementsDistinct(expectedLocations))
        ) && assert(filters)(
          hasField("countries", _.countries, hasSameElementsDistinct(expectedCountries))
        ) && assert(filters)(
          hasField("industries", _.industries, hasSameElementsDistinct(expectedIndustries))
        ) && assert(filters)(
          hasField("tags", _.tags, hasSameElementsDistinct(expectedTags))
        )
      },
      test("search") {
        val company1 = testCompany().copy(
          industry = Some(genString(8)),
          location = Some(genString(8)),
          country = Some(genString(8)),
          tags = genString(8) :: genString(8) :: Nil
        )
        val company2 = company1.copy(
          slug = company1.slug.drop(1),
          name = company1.name.drop(1),
          url = company1.url.drop(1)
        )
        val company3 = testCompany().copy(
          industry = None,
          location = Some(genString(8)),
          country = Some(genString(8)),
          tags = Nil
        )
        val companies = company1 :: company2 :: company3 :: Nil
        for
          repo <- ZIO.service[CompanyRepository]
          _    <- ZIO.foreachDiscard(companies)(repo.create)
          queryResult <- repo.search(
            CompanyFilter(
              locations = company1.location.get :: Nil,
              countries = company3.country.get :: Nil
            )
          )
        yield assert(queryResult.map(_.name))(hasSameElements(companies.map(_.name)))
      },
      test("search by tags") {
        val company1 = testCompany().copy(
          industry = Some(genString(8)),
          location = Some(genString(8)),
          country = Some(genString(8)),
          tags = genString(8) :: genString(8) :: Nil
        )
        val company2 = company1.copy(
          slug = company1.slug.drop(1),
          name = company1.name.drop(1),
          url = company1.url.drop(1)
        )
        val company3 = testCompany().copy(
          industry = None,
          location = Some(genString(8)),
          country = Some(genString(8)),
          tags = Nil
        )
        val companies = company1 :: company2 :: company3 :: Nil
        for
          repo        <- ZIO.service[CompanyRepository]
          _           <- ZIO.foreachDiscard(companies)(repo.create)
          queryResult <- repo.search(CompanyFilter(tags = company1.tags.take(1)))
        yield assert(queryResult.map(_.name))(
          hasSameElements((company1 :: company2 :: Nil).map(_.name))
        )
      }
    ).provide(CompanyRepository.live, dataSourceLayer, Repository.quillLayer, Scope.default)
