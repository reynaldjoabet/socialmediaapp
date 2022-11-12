package domain

import io.circe.generic.semiauto.deriveCodec
final case class Relationship(id: Int, follower_user_id: Int, followed_user_id: Int)

object Relationship {
  implicit val relationshipCodec = deriveCodec[Relationship]
}
