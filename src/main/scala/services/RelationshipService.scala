package services

import domain._
import cats.effect.kernel.Async
import doobie.util.transactor.Transactor
import doobie.implicits._

final case class RelationshipService[F[_]: Async](private val xa: Transactor[F]) {

  def getRelationships(followerUserId: Int): F[List[Relationship]] =
    sql"SELECT * FROM relationships WHERE follower_user_id= $followerUserId "
      .query[Relationship]
      .to[List]
      .transact(xa)

  def saveRelationship(
    follower_user_id: Int,
    followed_user_id: Int,
  ): F[Relationship] =
    sql"INSERT INTO relationships  (follower_user_id,followed_user_id) VALUES($follower_user_id,$followed_user_id)"
      .update
      .withUniqueGeneratedKeys[Relationship]("id", "follower_user_id", "followed_user_id")
      .transact(xa)

  def deleteRelationship(
    followerUserId: Int,
    followedUserId: Int,
  ): F[Int] =
    sql"delete from relationships where follower_user_id = $followerUserId AND followed_user_id=$followedUserId"
      .update
      .run
      .transact(xa)

}
