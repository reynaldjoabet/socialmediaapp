package api

import io.circe.generic.semiauto.deriveCodec
final case class CreateRelationship(follower_user_id: Int, followed_user_id: Int)

object CreateRelationship {
  implicit val createRelationshipCodec = deriveCodec[CreateRelationship]
}
