package api

import io.circe.generic.semiauto.deriveCodec
import org.http4s.circe.jsonOf
import cats.effect.IO

final case class CreateUser(
  username: String,
  email: String,
  password: String,
  name: String,
  coverPicture: Option[String],
  profilePicture: Option[String],
  city: Option[String],
  website: Option[String]
)

object CreateUser {

  implicit val registerCodec = deriveCodec[CreateUser]

}
