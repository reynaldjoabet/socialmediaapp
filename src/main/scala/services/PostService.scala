package services

import cats.effect.kernel.Async

import domain._
import doobie.implicits._
import doobie.util.transactor.Transactor

final case class PostService[F[_]: Async](private val xa: Transactor[F]) {

  def getPosts: F[List[Post]] =
    sql"SELECT p.*, u.id AS userId, name, profilePic FROM posts AS p JOIN users AS u ON (u.id = p.userId) LEFT JOIN relationships AS r ON (p.userId = r.followedUserId) WHERE r.followerUserId= ? OR p.userId =? ORDER BY p.createdAt DESC"
      .query[Post]
      .to[List]
      .transact(xa)

  def savePost(
    description: String,
    image: String,
    userId: Int
  ): F[Post] =
    sql"INSERT INTO posts  (descritpion,image,user_id) VALUES($description,$image,$userId)"
      .update
      .withUniqueGeneratedKeys[Post]("id", "description", "image", "user_id")
      .transact(xa)

  def deletePost(
    postId: Int,
    userId: Int
  ): F[Int] = sql"delete from posts where id = $postId AND user_id=$userId".update.run.transact(xa)

}
