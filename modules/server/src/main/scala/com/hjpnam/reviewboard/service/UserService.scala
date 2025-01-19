package com.hjpnam.reviewboard.service

import cats.data.OptionT
import com.hjpnam.reviewboard.domain.data.{User, UserToken}
import com.hjpnam.reviewboard.domain.error.{ObjectNotFound, Unauthorized}
import com.hjpnam.reviewboard.repository.{RecoveryTokenRepository, UserRepository}
import zio.*
import zio.interop.catz.core.*

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait UserService:
  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User]
  def deleteUser(email: String, password: String): Task[User]
  def generateToken(email: String, password: String): Task[Option[UserToken]]
  def sendPasswordRecoveryToken(email: String): Task[Unit]
  def recoverPassword(email: String, token: String, newPassword: String): Task[Boolean]

object UserService:
  val live = ZLayer {
    for {
      repository        <- ZIO.service[UserRepository]
      jwtService        <- ZIO.service[JWTService]
      emailService      <- ZIO.service[EmailService]
      recoveryTokenRepo <- ZIO.service[RecoveryTokenRepository]
    } yield new UserServiceLive(jwtService, emailService, repository, recoveryTokenRepo)
  }

class UserServiceLive(
    jwtService: JWTService,
    emailService: EmailService,
    userRepo: UserRepository,
    recoveryTokenRepo: RecoveryTokenRepository
) extends UserService:
  import UserServiceLive.Hasher.*

  override def registerUser(email: String, password: String): Task[User] =
    userRepo.create(User(id = -1L, email, hashedPassword = generateHash(password)))

  override def verifyPassword(email: String, password: String): Task[Boolean] = for
    maybeExistingUser <- userRepo
      .getByEmail(email)
    result <- maybeExistingUser.fold(ZIO.succeed(false))(existingUser =>
      ZIO.attempt(validateHash(password, existingUser.hashedPassword)).orElseSucceed(false)
    )
  yield result

  override def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User] =
    for
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(ObjectNotFound(s"cannot update user $email: user not found"))
      verified <- ZIO.attempt(validateHash(oldPassword, existingUser.hashedPassword))
      updatedUser <-
        if verified then updatePassword(newPassword, existingUser)
        else ZIO.fail(Unauthorized("wrong password"))
    yield updatedUser

  override def deleteUser(email: String, password: String): Task[User] =
    for
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(ObjectNotFound(s"cannot update user $email: user not found"))
      verified <- ZIO.attempt(validateHash(password, existingUser.hashedPassword))
      updatedUser <-
        if verified then userRepo.delete(existingUser.id)
        else ZIO.fail(Unauthorized("wrong password"))
    yield updatedUser

  override def generateToken(email: String, password: String): Task[Option[UserToken]] = for
    existingUser <- userRepo
      .getByEmail(email)
      .someOrFail(ObjectNotFound(s"cannot verify user $email: user not found"))
    verified   <- ZIO.attempt(validateHash(password, existingUser.hashedPassword))
    maybeToken <- jwtService.createToken(existingUser).when(verified)
  yield maybeToken

  override def sendPasswordRecoveryToken(email: String): Task[Unit] =
    OptionT(recoveryTokenRepo.getToken(email))
      .foldF(ZIO.unit)(token => emailService.sendPasswordRecoveryEmail(email, token))

  override def recoverPassword(email: String, token: String, newPassword: String): Task[Boolean] =
    for
      existingUser <- userRepo.getByEmail(email).someOrFail(ObjectNotFound("user not found"))
      tokenIsValid <- recoveryTokenRepo.checkToken(email, token)
      updateResult <- userRepo
        .update(existingUser.id, _.copy(hashedPassword = generateHash(newPassword)))
        .when(tokenIsValid)
        .map(_.nonEmpty)
    yield updateResult

  private def updatePassword(newPassword: String, existingUser: User) =
    userRepo
      .update(
        existingUser.id,
        user => user.copy(hashedPassword = generateHash(newPassword))
      )

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
