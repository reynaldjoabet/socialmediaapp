package domain

import io.circe.generic.semiauto.deriveCodec
final case class Likes(id: Int, userId: Int, postId: Int)

object Likes {
  implicit val likesCodec = deriveCodec[Likes]
}
