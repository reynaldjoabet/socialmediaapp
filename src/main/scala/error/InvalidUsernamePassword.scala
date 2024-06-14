package error

import cats.effect.IO

import io.circe.generic.semiauto.deriveCodec
import org.http4s.circe.jsonEncoderOf
import org.http4s.EntityEncoder

final case class InvalidUsernamePassword(message: String) extends Exception(message)

object InvalidUsernamePassword {
  implicit val codec = deriveCodec[InvalidUsernamePassword]

  // implicit  val entityEncoder1: EntityEncoder[IO,UserNotFound]=circeEntityEncoder

  // implicit  val entityEncoder1: EntityEncoder[IO,UserNotFound]=jsonEncoderOf[IO,UserNotFound]
}
