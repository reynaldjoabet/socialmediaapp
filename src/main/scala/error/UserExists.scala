package error

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

final case class UserExists(message: String) extends Exception(message)

object UserExists {
  implicit val codec = deriveCodec[UserExists]
}
