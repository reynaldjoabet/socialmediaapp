package api

import io.circe.generic.semiauto.deriveCodec
final case class CreateLike(userId: Int, postId: Int)

object CreateLike {
  implicit val createLikeCodec = deriveCodec[CreateLike]
}
