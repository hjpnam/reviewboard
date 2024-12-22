package com.hjpnam.reviewboard.http.controllers

import com.hjpnam.reviewboard.domain.data.Company
import com.hjpnam.reviewboard.http.endpoints.CompanyEndpoints
import sttp.tapir.ztapir.*
import zio.*

import scala.collection.mutable

class CompanyController private extends BaseController with CompanyEndpoints:
  val db = mutable.Map[Long, Company]()

  val create = createEndpoint.zServerLogic[Any] { req =>
    ZIO.succeed {
      val newId      = if db.keys.isEmpty then 1 else db.keys.max + 1
      val newCompany = req.toCompany(newId)
      db += newId -> newCompany
      newCompany
    }
  }

  val getAll = getAllEndpoint.zServerLogic[Any] { _ =>
    ZIO.succeed(db.values.toList)
  }

  val getById = getByIdEndpoint.zServerLogic[Any] { id =>
    ZIO
      .attempt(id.toLong)
      .mapBoth(_ => None, db.get)
  }

  override val routes: List[ZServerEndpoint[Any, Any]] = create :: getAll :: getById :: Nil

object CompanyController:
  val makeZIO: UIO[CompanyController] = ZIO.succeed(new CompanyController)
