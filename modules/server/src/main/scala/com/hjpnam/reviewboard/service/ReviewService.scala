package com.hjpnam.reviewboard.service

import com.hjpnam.reviewboard.domain.data.Review
import com.hjpnam.reviewboard.http.request.CreateReviewRequest
import com.hjpnam.reviewboard.repository.ReviewRepository
import zio.*

import java.time.Instant

trait ReviewService:
  def create(createRequest: CreateReviewRequest, userId: Long): Task[Review]
  def update(id: Long, updateRequest: CreateReviewRequest): Task[Review]
  def delete(id: Long): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def getAll: Task[List[Review]]

object ReviewService:
  val live = ZLayer {
    for repo <- ZIO.service[ReviewRepository]
    yield new ReviewServiceLive(repo)
  }

class ReviewServiceLive(repository: ReviewRepository) extends ReviewService:
  override def create(createRequest: CreateReviewRequest, userId: Long): Task[Review] =
    repository.create(createRequest.toReview(-1L, userId, Instant.now()))

  override def update(id: Long, updateRequest: CreateReviewRequest): Task[Review] =
    repository.update(id, _ => updateRequest.toReview(id, -1L, Instant.now()))

  override def delete(id: Long): Task[Review] = repository.delete(id)

  override def getById(id: Long): Task[Option[Review]] = repository.getById(id)

  override def getByCompanyId(companyId: Long): Task[List[Review]] =
    repository.getByCompanyId(companyId)

  override def getByUserId(userId: Long): Task[List[Review]] = repository.getByUserId(userId)

  override def getAll: Task[List[Review]] = repository.get
