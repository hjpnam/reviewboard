package com.hjpnam.reviewboard.repository

import com.hjpnam.reviewboard.domain.data.Review
import io.getquill.*
import io.getquill.jdbczio.*
import zio.{Task, URLayer, ZIO, ZLayer}

trait ReviewRepository:
  def create(review: Review): Task[Review]
  def update(id: Long, op: Review => Review): Task[Review]
  def delete(id: Long): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def get: Task[List[Review]]

object ReviewRepository:
  val live: URLayer[Quill.Postgres[SnakeCase], ReviewRepository] = ZLayer {
    for quill <- ZIO.service[Quill.Postgres[SnakeCase]]
    yield new ReviewRepositoryLive(quill)
  }

class ReviewRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends ReviewRepository:
  import quill.*

  inline given schema: SchemaMeta[Review]  = schemaMeta[Review]("review")
  inline given insMeta: InsertMeta[Review] = insertMeta[Review](_.id, _.created, _.updated)
  inline given updMeta: UpdateMeta[Review] = updateMeta[Review](_.id, _.created)

  override def create(review: Review): Task[Review] = run(
    query[Review]
      .insertValue(lift(review))
      .returning(r => r)
  )

  override def update(id: Long, op: Review => Review): Task[Review] =
    for
      currentReview <- getById(id).someOrFail(new RuntimeException(s"row not found for id: $id"))
      updatedReview <- run(
        query[Review]
          .filter(_.id == lift(id))
          .updateValue(lift(op(currentReview)))
          .returning(r => r)
      )
    yield updatedReview

  override def delete(id: Long): Task[Review] = run(
    query[Review]
      .filter(_.id == lift(id))
      .delete
      .returning(r => r)
  )

  override def getById(id: Long): Task[Option[Review]] = run(
    query[Review]
      .filter(_.id == lift(id))
  ).map(_.headOption)

  override def getByUserId(userId: Long): Task[List[Review]] = run(
    query[Review]
      .filter(_.userId == lift(userId))
  )

  override def getByCompanyId(companyId: Long): Task[List[Review]] = run(
    query[Review].filter(_.companyId == lift(companyId))
  )

  override def get: Task[List[Review]] = run(query[Review])
