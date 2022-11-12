package api

import io.circe.generic.semiauto.deriveCodec
final case class CreateStory(imageUrl: String, userId: Int)

object CreateStory {
  implicit val createStoryCodec = deriveCodec[CreateStory]
}
