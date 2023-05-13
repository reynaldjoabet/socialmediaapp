package routes

import org.http4s.dsl.Http4sDsl
import cats.effect.kernel.Async
import org.http4s.server.AuthMiddleware
import api._
import authorization.Auth0AuthorizationMiddleware
import org.http4s._
import services._
import db.Doobie._
import doobie.util.transactor._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
import cats.implicits._

final case class StoryRoutes[F[_]: Async](storyService: StoryService[F]) extends Http4sDsl[F] {
  import org.http4s.server.Router
  private object UserId extends QueryParamDecoderMatcher[Int]("userId")
  private object StoryId extends QueryParamDecoderMatcher[Int]("storyId")
  private val prefix = "/api/story"

  private val routes = HttpRoutes.of[F] {
    case GET -> Root / IntVar(userId) =>
      storyService
        .getStories(userId)
        .flatMap(Ok(_))
        .handleErrorWith(e => InternalServerError(e.toString()))
    case req @ POST -> Root / "add" =>
      req
        .as[CreateStory]
        .flatMap { story =>
          storyService
            .saveStory(story.imageUrl, story.userId)
            .flatMap(Ok(_))
        }
        .handleErrorWith(e => InternalServerError(e.toString()))
    case DELETE -> Root / IntVar(userId) / IntVar(storyId) =>
      storyService
        .deleteStory(storyId, userId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))
    // .map(_.addCookie())
  }

  private val authRoutes = AuthedRoutes.of[LoginUser, F] {
    case GET -> Root / IntVar(userId) as loginUser =>
      storyService
        .getStories(userId)
        .flatMap(Ok(_))
        .handleErrorWith(e => InternalServerError(e.toString()))

    case req @ POST -> Root / "add" as loginUser =>
      req
        .req
        .as[CreateStory]
        .flatMap { story =>
          storyService
            .saveStory(
              story.imageUrl,
              story.userId
            )
            .flatMap(Ok(_))
        }
        .handleErrorWith(e => InternalServerError(e.toString()))

    case DELETE -> Root / IntVar(userId) / IntVar(storyId) as loginUser =>
      storyService
        .deleteStory(storyId, userId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))

    case DELETE -> Root / "delete" :? UserId(userId) +& StoryId(storyId) as loginUser =>
      storyService
        .deleteStory(storyId, userId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))

    // .map(_.addCookie())
  }

  val storyRoutes = Router(
    prefix -> Auth0AuthorizationMiddleware(routes)
  )

  def routes(authMiddleware: AuthMiddleware[F, LoginUser]): HttpRoutes[F] = Router(
    prefix -> authMiddleware(authRoutes)
  )

}

object StoryRoutes {
  def make[F[_]: Async](): StoryRoutes[F] = StoryRoutes[F](StoryService(xa))

  def make[F[_]: Async](transactor: Transactor[F]): StoryRoutes[F] = StoryRoutes[F](
    StoryService(transactor)
  )

}
