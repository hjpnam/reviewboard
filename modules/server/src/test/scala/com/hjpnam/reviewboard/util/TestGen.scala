package com.hjpnam.reviewboard.util

import com.hjpnam.reviewboard.domain.data.Company
import zio.test.magnolia.DeriveGen
import zio.test.Gen

import scala.util.Random

trait TestGen:
  def genString(length: Int): String = Random.alphanumeric.take(length).mkString
  def genLong(): Long                = Random.nextLong()
  def genRating(): Int               = scala.util.Random.between(1, 5)
  val genCompany: Gen[Any, Company]  = DeriveGen[Company]
