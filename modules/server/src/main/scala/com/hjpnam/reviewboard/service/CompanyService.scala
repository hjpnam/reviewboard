package com.hjpnam.reviewboard.service

import com.hjpnam.reviewboard.domain.data.{Company, CompanyFilter}
import com.hjpnam.reviewboard.http.request.CreateCompanyRequest
import com.hjpnam.reviewboard.repository.CompanyRepository
import zio.{Task, URLayer, ZIO, ZLayer}

trait CompanyService:
  def create(createRequest: CreateCompanyRequest): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def allFilters: Task[CompanyFilter]

object CompanyService:
  val live: URLayer[CompanyRepository, CompanyServiceLive] = ZLayer {
    for repository <- ZIO.service[CompanyRepository]
    yield new CompanyServiceLive(repository)
  }

class CompanyServiceLive(companyRepository: CompanyRepository) extends CompanyService:
  override def create(createRequest: CreateCompanyRequest): Task[Company] =
    companyRepository.create(createRequest.toCompany(-1))

  override def getAll: Task[List[Company]] =
    companyRepository.get

  override def getById(id: Long): Task[Option[Company]] =
    companyRepository.getById(id)

  override def getBySlug(slug: String): Task[Option[Company]] =
    companyRepository.getBySlug(slug)

  override def allFilters: Task[CompanyFilter] = companyRepository.uniqueAttributes
