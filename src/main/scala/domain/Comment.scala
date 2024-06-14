package domain

import java.time.LocalDateTime

import io.circe.generic.semiauto.deriveCodec

final case class Comment(
  id: Int,
  description: String,
  createdAt: LocalDateTime,
  userId: Int,
  postId: Int
)

object Comment {
  implicit val commetCodec = deriveCodec[Comment]
}
