package domain

import io.circe.generic.semiauto.deriveCodec
final case class Post(id: Int, description: String, image: String, userId: Int)

object Post {
  implicit val postCodec = deriveCodec[Post]
}
