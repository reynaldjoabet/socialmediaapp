package api

import java.time.LocalDateTime
import io.circe.generic.semiauto.deriveCodec

final case class CreateComment(
  description: String,
  createdAt: LocalDateTime,
  userId: Int,
  postId: Int,
)

object CreateComment {
  implicit val createCommentCodec = deriveCodec[CreateComment]
}
