package error

import io.circe.generic.semiauto.deriveCodec
import org.http4s.circe.jsonEncoderOf
import cats.effect.IO
import org.http4s.EntityEncoder
final case class UserNotFound(message: String) extends Exception(message)

object UserNotFound {
  implicit val codec = deriveCodec[UserNotFound]

  // implicit  val entityEncoder1: EntityEncoder[IO,UserNotFound]=circeEntityEncoder

  // implicit  val entityEncoder1: EntityEncoder[IO,UserNotFound]=jsonEncoderOf[IO,UserNotFound]
}
