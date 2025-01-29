package com.hjpnam.reviewboard.repository

import com.hjpnam.reviewboard.domain.data.{Company, CompanyFilter}
import com.hjpnam.reviewboard.domain.error.ObjectNotFound
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.{Task, URLayer, ZIO, ZLayer}

trait CompanyRepository:
  def create(company: Company): Task[Company]
  def update(id: Long, op: Company => Company): Task[Company]
  def delete(id: Long): Task[Company]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def get: Task[List[Company]]
  def uniqueAttributes: Task[CompanyFilter]

object CompanyRepository:
  val live: URLayer[Quill.Postgres[SnakeCase.type], CompanyRepositoryLive] = ZLayer {
    for quill <- ZIO.service[Quill.Postgres[SnakeCase.type]]
    yield new CompanyRepositoryLive(quill)
  }

class CompanyRepositoryLive(quill: Quill.Postgres[SnakeCase.type]) extends CompanyRepository:
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
      current <- getById(id).someOrFail(ObjectNotFound(s"row not found for id: $id"))
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

  override def uniqueAttributes: Task[CompanyFilter] =
    for
      locations  <- run(query[Company].map(_.location).distinct).map(_.flatMap(_.toList))
      countries  <- run(query[Company].map(_.country).distinct).map(_.flatMap(_.toList))
      industries <- run(query[Company].map(_.industry).distinct).map(_.flatMap(_.toList))
      tags       <- run(query[Company].map(_.tags).distinct).map(_.flatten.distinct)
    yield CompanyFilter(locations, countries, industries, tags)
