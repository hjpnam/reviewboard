package com.hjpnam.reviewboard.repository

import com.hjpnam.reviewboard.config.RecoveryTokenConfig
import com.hjpnam.reviewboard.fixture.{RepoStub, TestObject}
import com.hjpnam.reviewboard.repository.CompanyRepositorySpec.dataSourceLayer
import zio.test.*
import zio.test.Assertion.*
import zio.{Scope, ZIO, ZLayer}

object RecoveryTokenRepositorySpec extends ZIOSpecDefault, RepositorySpec, TestObject, RepoStub:
  override val initScriptPath = "sql/recovery-token.sql"

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("RecoveryTokenRepositorySpec")(
      test("generate a token") {
        for
          repo  <- ZIO.service[RecoveryTokenRepository]
          token <- repo.getToken(testEmail)
        yield assert(token)(isSome(hasSizeString(equalTo(8))))
      },
      test("not generate a token if user is not found") {
        for
          repo  <- ZIO.service[RecoveryTokenRepository]
          token <- repo.getToken(testEmail.drop(1))
        yield assert(token)(isNone)
      },
      test("replace a token if token already exists for email") {
        for
          repo     <- ZIO.service[RecoveryTokenRepository]
          oldToken <- repo.getToken(testEmail)
          newToken <- repo.getToken(testEmail)
        yield assert(oldToken)(isSome(hasSizeString(equalTo(8))))
          && assert(newToken)(isSome(hasSizeString(equalTo(8))))
          && assertTrue(newToken != oldToken)
      },
      test("checks if token exists") {
        for
          repo       <- ZIO.service[RecoveryTokenRepository]
          firstCheck <- repo.checkToken(testEmail, "foo")
          token <- repo
            .getToken(testEmail)
            .someOrFail(new RuntimeException("token expected to be generated"))
          secondCheck <- repo.checkToken(testEmail, token)
        yield assertTrue(!firstCheck, secondCheck)
      }
    ).provide(
      RecoveryTokenRepository.live,
      stubUserRepoLayer,
      dataSourceLayer,
      Repository.quillLayer,
      ZLayer.succeed(RecoveryTokenConfig(60000)),
      Scope.default
    )
