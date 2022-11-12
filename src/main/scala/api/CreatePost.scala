package api

import io.circe.generic.semiauto.deriveCodec
final case class CreatePost(description: String, image: String, userId: Int)

object CreatePost {
  implicit val createPostCodec = deriveCodec[CreatePost]
}
