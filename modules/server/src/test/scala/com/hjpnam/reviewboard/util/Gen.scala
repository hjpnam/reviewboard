package com.hjpnam.reviewboard.util

import scala.util.Random

trait Gen:
  def genString(length: Int): String = Random.alphanumeric.take(length).mkString
  def genLong(): Long                = Random.nextLong()
  def genRating(): Int               = scala.util.Random.between(1, 5)
