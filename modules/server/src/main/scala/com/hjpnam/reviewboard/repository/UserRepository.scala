package com.hjpnam.reviewboard.repository

import com.hjpnam.reviewboard.domain.data.User
import com.hjpnam.reviewboard.domain.error.ObjectNotFound
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.{Task, URLayer, ZIO, ZLayer}

trait UserRepository:
  def create(user: User): Task[User]
  def update(id: Long, op: User => User): Task[User]
  def getById(id: Long): Task[Option[User]]
  def getByEmail(email: String): Task[Option[User]]
  def delete(id: Long): Task[User]

object UserRepository:
  val live: URLayer[Quill.Postgres[SnakeCase.type], UserRepositoryLive] = ZLayer {
    for quill <- ZIO.service[Quill.Postgres[SnakeCase.type]]
    yield new UserRepositoryLive(quill)
  }

class UserRepositoryLive(quill: Quill.Postgres[SnakeCase.type]) extends UserRepository:
  import quill.*

  inline given schema: SchemaMeta[User]  = schemaMeta[User]("usr")
  inline given insMeta: InsertMeta[User] = insertMeta[User](_.id)
  inline given updMeta: UpdateMeta[User] = updateMeta[User](_.id)

  override def create(user: User): Task[User] = run {
    query[User]
      .insertValue(lift(user))
      .returning(r => r)
  }

  override def update(id: Long, op: User => User): Task[User] = for
    current <- getById(id).someOrFail(ObjectNotFound(s"row not found for id: $id"))
    updated <- run {
      query[User]
        .filter(_.id == lift(id))
        .updateValue(lift(op(current)))
        .returning(r => r)
    }
  yield updated

  override def getById(id: Long): Task[Option[User]] = run {
    query[User].filter(_.id == lift(id))
  }.map(_.headOption)

  override def getByEmail(email: String): Task[Option[User]] = run {
    query[User].filter(_.email == lift(email))
  }.map(_.headOption)

  override def delete(id: Long): Task[User] = run {
    query[User]
      .filter(_.id == lift(id))
      .delete
      .returning(r => r)
  }
