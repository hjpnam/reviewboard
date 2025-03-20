package com.hjpnam.reviewboard.core

import com.hjpnam.reviewboard.domain.data.UserToken
import com.raquo.laminar.api.L.{*, given}

import scala.scalajs.js
import scala.scalajs.js.Date

object Session:
  val stateName: String                 = "userState"
  val userState: Var[Option[UserToken]] = Var(None)

  def isActive: Boolean =
    loadUserState()
    userState.now().nonEmpty

  def setUserState(token: UserToken): Unit =
    userState.set(Some(token))
    Storage.set("userState", token)

  def loadUserState(): Unit =
    Storage
      .get[UserToken](stateName)
      .filter(_.expires * 1000 < new Date().getTime())
      .foreach(_ => Storage.remove(stateName))

    userState.set(
      Storage.get[UserToken](stateName)
    )

  def clearUserState(): Unit =
    Storage.remove(stateName)
    userState.set(None)

  def getUserState(): Option[UserToken] =
    loadUserState()
    userState.now()
