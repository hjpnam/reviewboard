package com.hjpnam.reviewboard.service

import com.hjpnam.reviewboard.domain.data.Company
import com.hjpnam.reviewboard.http.request.CreateCompanyRequest
import zio.*

import scala.collection.mutable

trait CompanyService:
  def create(createRequest: CreateCompanyRequest): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]

object CompanyService:
  val dummyLayer: ULayer[CompanyServiceDummy] = ZLayer.succeed(new CompanyServiceDummy)

class CompanyServiceDummy extends CompanyService:
  val db = mutable.Map.empty[Long, Company]

  override def create(createRequest: CreateCompanyRequest): Task[Company] =
    ZIO.succeed {
      val newId      = if db.keys.isEmpty then 1 else db.keys.max + 1
      val newCompany = createRequest.toCompany(newId)
      db += newId -> newCompany
      newCompany
    }

  override def getAll: Task[List[Company]] = ZIO.succeed(db.values.toList)

  override def getById(id: Long): Task[Option[Company]] = ZIO.succeed {
    db.get(id)
  }

  override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed {
    db.values.find(_.slug == slug)
  }
