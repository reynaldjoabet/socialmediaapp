package services

import cats.effect.kernel.Async

import domain._
import doobie.implicits._
import doobie.util.transactor.Transactor

final case class LikesService[F[_]: Async](private val xa: Transactor[F]) {

  def getLikes(postId: Int): F[List[Likes]] = sql"SELECT * FROM likes WHERE post_id= $postId "
    .query[Likes]
    .to[List]
    .transact(xa)

  def saveLikes(userId: Int, postId: Int): F[Likes] =
    sql"INSERT INTO likes  (user_id,post_id) VALUES($userId,$postId)"
      .update
      .withUniqueGeneratedKeys[Likes]("id", "user_id", "post_id")
      .transact(xa)

  def deleteLikes(
    userId: Int,
    postId: Int
  ): F[Int] = sql"delete from likes where user_id = $userId AND post_id=$postId"
    .update
    .run
    .transact(xa)

}
