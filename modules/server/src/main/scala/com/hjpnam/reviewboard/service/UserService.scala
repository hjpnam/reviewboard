package com.hjpnam.reviewboard.service

import com.hjpnam.reviewboard.domain.data.{User, UserToken}
import com.hjpnam.reviewboard.domain.error.ObjectNotFound
import com.hjpnam.reviewboard.repository.UserRepository
import zio.*

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait UserService:
  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def generateToken(email: String, password: String): Task[Option[UserToken]]

object UserService:
  val live: URLayer[JWTService & UserRepository, UserServiceLive] = ZLayer {
    for {
      repository <- ZIO.service[UserRepository]
      jwtService <- ZIO.service[JWTService]
    } yield new UserServiceLive(jwtService, repository)
  }

class UserServiceLive(jwtService: JWTService, userRepo: UserRepository) extends UserService:
  import UserServiceLive.Hasher.*

  override def registerUser(email: String, password: String): Task[User] =
    userRepo.create(User(id = -1L, email, hashedPassword = generateHash(password)))

  override def verifyPassword(email: String, password: String): Task[Boolean] = for
    existingUser <- userRepo
      .getByEmail(email)
      .someOrFail(ObjectNotFound(s"cannot verify user $email not found"))
    result <- ZIO.attempt(validateHash(password, existingUser.hashedPassword))
  yield result

  override def generateToken(email: String, password: String): Task[Option[UserToken]] = for
    existingUser <- userRepo
      .getByEmail(email)
      .someOrFail(new ObjectNotFound(s"cannot verify user $email: user not found"))
    verified   <- ZIO.attempt(validateHash(password, existingUser.hashedPassword))
    maybeToken <- jwtService.createToken(existingUser).when(verified)
  yield maybeToken

object UserServiceLive:
  object Hasher:
    private val PBKDF2_ALGORITHM: String = "PBKDF2WithHmacSHA512"
    private val PBKDF2_ITERATIONS: Int   = 1000
    private val SALT_BYTE_SIZE: Int      = 24
    private val HASH_BYTE_SIZE: Int      = 24
    private val skf: SecretKeyFactory    = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

    private def pbkdf2(
        message: Array[Char],
        salt: Array[Byte],
        iterations: Int,
        nBytes: Int
    ): Array[Byte] =
      val keySpec: PBEKeySpec = new PBEKeySpec(message, salt, iterations, nBytes * 8)
      skf.generateSecret(keySpec).getEncoded

    private def toHex(array: Array[Byte]): String = array.map(byte => "%02x".format(byte)).mkString
    private def fromHex(string: String): Array[Byte] =
      string
        .sliding(2, 2)
        .map(hexValue => Integer.parseInt(hexValue, 16).toByte)
        .toArray
    private def compareBytes(a: Array[Byte], b: Array[Byte]): Boolean =
      val range = 0 until a.length.min(b.length)
      val diff = range.foldLeft(a.length ^ b.length) { case (acc, i) =>
        acc | (a(i) ^ b(i))
      }
      diff == 0

    def generateHash(string: String): String =
      val rng: SecureRandom = new SecureRandom()
      val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
      rng.nextBytes(salt)
      val hashBytes = pbkdf2(string.toCharArray, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
      s"$PBKDF2_ITERATIONS:${toHex(salt)}:${toHex(hashBytes)}"

    def validateHash(string: String, hash: String): Boolean =
      val hashSegments = hash.split(":")
      val nIterations  = hashSegments.head
      val salt         = fromHex(hashSegments(1))
      val validHash    = fromHex(hashSegments.last)
      val testHash     = pbkdf2(string.toCharArray, salt, nIterations.toInt, HASH_BYTE_SIZE)
      compareBytes(testHash, validHash)
