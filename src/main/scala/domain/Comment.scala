package domain

import java.time.LocalDateTime

final case class Comment(
  id: Int,
  description: String,
  createdAt: LocalDateTime,
  userId: Int,
  postId: Int,
)
