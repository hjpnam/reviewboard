package com.hjpnam.reviewboard.component

import scala.scalajs.js
import scala.scalajs.js.annotation
import scala.scalajs.js.annotation.{JSGlobal, JSImport}

@js.native
@JSGlobal
class Moment extends js.Object:
  def format(): String  = js.native
  def fromNow(): String = js.native

@js.native
@JSImport("moment", JSImport.Default)
object MomentLib extends js.Object:
  def unix(millis: Long): Moment = js.native

object Time:
  def unix2Hr(millis: Long): String = MomentLib.unix(millis / 1000).fromNow()
