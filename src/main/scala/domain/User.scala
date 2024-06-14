package domain

import cats.effect.IO

import io.circe.generic.semiauto.deriveCodec

final case class User(
  id: Int,
  username: String,
  email: String,
  password: String,
  name: String,
  coverPicture: Option[String],
  profilePicture: Option[String],
  city: Option[String],
  website: Option[String]
)

object User {

  implicit val registerCodec = deriveCodec[User]

}
