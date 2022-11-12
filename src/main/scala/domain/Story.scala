package domain

import io.circe.generic.semiauto.deriveCodec
final case class Story(id: Int, imageUrl: String, userId: Int)

object Story {
  implicit val storyCodec = deriveCodec[Story]
}
