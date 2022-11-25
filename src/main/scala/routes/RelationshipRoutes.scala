package routes

import cats.effect.kernel.Async
import services._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import api._
import org.http4s._
import db.Doobie._
import authentication.AuthenticationMiddleware
import doobie.util.transactor._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
import cats.implicits._

final case class RelationshipRoutes[F[_]: Async](relationshipService: RelationshipService[F])
  extends Http4sDsl[F] {
  import org.http4s.server.Router

  private object UserId extends QueryParamDecoderMatcher[Int]("userId")
  private object RelationshipId extends QueryParamDecoderMatcher[Int]("relationshipId")
  private val prefix = "api/relationships"

  private val routes = HttpRoutes.of[F] {
    case GET -> Root / IntVar(followerUserId) =>
      relationshipService
        .getRelationships(followerUserId)
        .flatMap(Ok(_))
        .handleErrorWith(e => InternalServerError(e.toString()))
    case req @ POST -> Root / "add" =>
      req
        .as[CreateRelationship]
        .flatMap { relationship =>
          relationshipService
            .saveRelationship(relationship.follower_user_id, relationship.followed_user_id)
            .flatMap(Ok(_))
        }
        .handleErrorWith(e => InternalServerError(e.toString()))
    case DELETE -> Root / IntVar(userId) / IntVar(relationshipId) =>
      relationshipService
        .deleteRelationship(relationshipId, userId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))
    // .map(_.addCookie())
  }

  private val authRoutes = AuthedRoutes.of[LoginUser, F] {
    case GET -> Root / IntVar(followerUserId) as loginUser =>
      relationshipService
        .getRelationships(followerUserId)
        .flatMap(Ok(_))
        .handleErrorWith(e => InternalServerError(e.toString()))

    case req @ POST -> Root / "add" as loginUser =>
      req
        .req
        .as[CreateRelationship]
        .flatMap { relationship =>
          relationshipService
            .saveRelationship(
              relationship.follower_user_id,
              relationship.followed_user_id,
            )
            .flatMap(Ok(_))
        }
        .handleErrorWith(e => InternalServerError(e.toString()))

    case DELETE -> Root / IntVar(userId) / IntVar(relationshipId) as loginUser =>
      relationshipService
        .deleteRelationship(relationshipId, userId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))

    case DELETE -> Root / "delete" :? UserId(userId) +& RelationshipId(
          relationshipId
        ) as loginUser =>
      relationshipService
        .deleteRelationship(relationshipId, userId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))

    // .map(_.addCookie())
  }

  val commentRoutes = Router(
    prefix -> AuthenticationMiddleware(routes)
  )

  def routes(authMiddleware: AuthMiddleware[F, LoginUser]): HttpRoutes[F] = Router(
    prefix -> authMiddleware(authRoutes)
  )

}

object RelationshipRoutes {
  def make[F[_]: Async]() = RelationshipRoutes[F](RelationshipService(xa))

  def make[F[_]: Async](
    transactor: Transactor[F]
  ) = RelationshipRoutes[F](RelationshipService(transactor))

}
