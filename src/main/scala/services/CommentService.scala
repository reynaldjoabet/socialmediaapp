package services

import domain._
import cats.effect.kernel.Async
import doobie.util.transactor.Transactor
import doobie.implicits._
import java.time.LocalDateTime
//import doobie.implicits.javasql._
//import doobie.implicits.javatimedrivernative
import doobie.postgres.implicits._

final case class CommentService[F[_]: Async](private val xa: Transactor[F]) {

  def getComments(postId: Int): F[List[Comment]] =
    sql"SELECT * FROM comments  JOIN users ON comments.user_id=users.id WHERE comments.post_id=$postId ORDER BY comments.created_at DESC"
      .query[Comment]
      .to[List]
      .transact(xa)

  def saveComment(
    description: String,
    createdAt: LocalDateTime,
    userId: Int,
    postId: Int
  ): F[Comment] =
    sql"INSERT INTO comments  (description,created_at,user_id,post_id) VALUES($description,$createdAt,$userId,$postId)"
      .update
      .withUniqueGeneratedKeys[Comment]("id", "description", "created_at", "user_id", "post_id")
      .transact(xa)

  def deleteComment(
    commentId: Int,
    userId: Int
  ): F[Int] = sql"delete from comments where id = $commentId AND user_id=$userId"
    .update
    .run
    .transact(xa)

}
