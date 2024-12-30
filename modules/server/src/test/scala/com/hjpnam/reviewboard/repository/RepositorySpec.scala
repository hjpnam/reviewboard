package com.hjpnam.reviewboard.repository

import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import zio.{ZIO, ZLayer}

trait RepositorySpec:
  def initScriptPath: String

  def createPostgresContainer(): PostgreSQLContainer[Nothing] =
    val container: PostgreSQLContainer[Nothing] =
      PostgreSQLContainer("postgres").withInitScript(initScriptPath)
    container.start()
    container

  def createDataSource(container: PostgreSQLContainer[Nothing]): PGSimpleDataSource =
    val dataSource = new PGSimpleDataSource()
    dataSource.setURL(container.getJdbcUrl())
    dataSource.setUser(container.getUsername())
    dataSource.setPassword(container.getPassword())
    dataSource

  val dataSourceLayer = ZLayer {
    for
      container <- ZIO.acquireRelease(ZIO.attempt(createPostgresContainer()))(container =>
        ZIO.attempt(container.stop()).ignoreLogged
      )
      dataSource <- ZIO.attempt(createDataSource(container))
    yield dataSource
  }
