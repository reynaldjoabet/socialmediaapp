package api

import io.circe.generic.semiauto.deriveCodec
import org.http4s.circe.jsonOf
import cats.effect.IO

final case class RegisterUser(
  username: String,
  email: String,
  password: String,
  name: String,
  coverPicture: Option[String],
  profilePicture: Option[String],
  city: Option[String],
  website: Option[String],
)

object RegisterUser {

  implicit val registerCodec = deriveCodec[RegisterUser]

}
