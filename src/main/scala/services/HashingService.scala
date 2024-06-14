package services

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

import org.mindrot.jbcrypt.BCrypt
import pdi.jwt.JwtCirce

object HashingService {

  def hashCookieContent(content: String) = {
    val random = new SecureRandom
    val salt   = new Array[Byte](16)
    random.nextBytes(salt)
    val messageDigest = MessageDigest.getInstance("SHA-512")
    messageDigest.update(salt)
    val hashedCookie = messageDigest.digest(content.getBytes(StandardCharsets.UTF_8))
    Base64.getEncoder().encode(hashedCookie)

  }

  def hashCirce(username: String) = JwtCirce.encode("username", username)

  def hashPassword(passwordToHash: String): String = BCrypt.hashpw(passwordToHash, BCrypt.gensalt())

  def checkPassword(
    hash: String,
    passwordToHash: String
  ): Boolean = BCrypt.checkpw(passwordToHash, hash)

}
