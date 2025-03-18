package com.hjpnam.reviewboard.core

import com.hjpnam.reviewboard.domain.data.UserToken
import scalajs.js.Date
import com.raquo.laminar.api.L.{*, given}

object Session:
  val stateName: String                 = "userState"
  val userState: Var[Option[UserToken]] = Var(None)

  def isActive = userState.now().nonEmpty

  def setUserState(token: UserToken): Unit =
    userState.set(Some(token))
    Storage.set("userState", token)

  def loadUserState(): Unit =
    Storage
      .get[UserToken](stateName)
      .filter(_.expires * 1000 > new Date().getTime())
      .foreach(_ => Storage.remove(stateName))

    userState.set(
      Storage.get[UserToken](stateName)
    )
