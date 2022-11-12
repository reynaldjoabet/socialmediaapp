package api

import io.circe.generic.semiauto.deriveCodec
import org.http4s.circe.jsonOf
import cats.effect.IO
final case class LoginUser(username: String, password: String)

object LoginUser {

  implicit val loginCodec = deriveCodec[LoginUser]

  implicit val loginEntityDecoder = jsonOf[IO, LoginUser]

}
