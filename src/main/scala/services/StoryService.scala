package services

import domain._
import cats.effect.kernel.Async
import doobie.util.transactor.Transactor
import doobie.implicits._

final case class StoryService[F[_]: Async](private val xa: Transactor[F]) {

  def getStories(userId: Int): F[List[Story]] =
    sql"SELECT * FROM stories JOIN users ON stories.user_id=users.id "
      .query[Story]
      .to[List]
      .transact(xa)

  def saveStory(
    imageUrl: String,
    userId: Int
  ): F[Story] = sql"INSERT INTO stories  (image_url,user_id) VALUES($imageUrl,$userId)"
    .update
    .withUniqueGeneratedKeys[Story]("id", "image_url", "user_id")
    .transact(xa)

  def deleteStory(
    storyId: Int,
    userId: Int
  ): F[Int] = sql"delete from stories where id = $storyId AND user_id=$userId"
    .update
    .run
    .transact(xa)

}
