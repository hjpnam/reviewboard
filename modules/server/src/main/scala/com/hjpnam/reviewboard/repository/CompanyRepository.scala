package com.hjpnam.reviewboard.repository

import com.hjpnam.reviewboard.domain.data.Company
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait CompanyRepository:
  def create(company: Company): Task[Company]
  def update(id: Long, op: Company => Company): Task[Company]
  def delete(id: Long): Task[Company]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def get: Task[List[Company]]

object CompanyRepository:
  val live: URLayer[Quill.Postgres[SnakeCase], CompanyRepositoryLive] = ZLayer {
    for quill <- ZIO.service[Quill.Postgres[SnakeCase]]
    yield new CompanyRepositoryLive(quill)
  }

class CompanyRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends CompanyRepository:
  import quill.*

  inline given schema: SchemaMeta[Company]  = schemaMeta[Company]("company")
  inline given insMeta: InsertMeta[Company] = insertMeta[Company](_.id)
  inline given updMeta: UpdateMeta[Company] = updateMeta[Company](_.id)

  override def create(company: Company): Task[Company] = run {
    query[Company]
      .insertValue(lift(company))
      .returning(r => r)
  }

  override def update(id: Long, op: Company => Company): Task[Company] =
    for
      current <- getById(id).someOrFail(new RuntimeException(s"row not found for id: $id"))
      updated <- run {
        query[Company]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(r => r)
      }
    yield updated

  override def delete(id: Long): Task[Company] = run {
    query[Company]
      .filter(_.id == lift(id))
      .delete
      .returning(r => r)
  }

  override def getById(id: Long): Task[Option[Company]] = run {
    query[Company].filter(_.id == lift(id))
  }.map(_.headOption)

  override def getBySlug(slug: String): Task[Option[Company]] = run {
    query[Company].filter(_.slug == lift(slug))
  }.map(_.headOption)

  override def get: Task[List[Company]] = run(query[Company])
