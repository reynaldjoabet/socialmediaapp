package routes

import org.http4s.dsl.Http4sDsl
import org.http4s._
import cats.effect.kernel.Async
import org.http4s.server.AuthMiddleware
import services._
import api._
import authorization.Auth0AuthorizationMiddleware
import db.Doobie._
import doobie.util.transactor._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
import cats.implicits._

final case class PostRoutes[F[_]: Async](postService: PostService[F]) extends Http4sDsl[F] {

  import org.http4s.server.Router
  private object UserId extends QueryParamDecoderMatcher[Int]("userId")
  private object PostId extends QueryParamDecoderMatcher[Int]("postId")
  private val prefix = "/api/posts"

  private val routes = HttpRoutes.of[F] {
    case GET -> Root =>
      postService
        .getPosts
        .flatMap(Ok(_))
        .handleErrorWith(e => InternalServerError(e.toString()))
    case req @ POST -> Root / "add" =>
      req
        .as[CreatePost]
        .flatMap { post =>
          postService
            .savePost(post.description, post.image, post.userId)
            .flatMap(Ok(_))
        }
        .handleErrorWith(e => InternalServerError(e.toString()))
    case DELETE -> Root / IntVar(userId) / IntVar(postId) =>
      postService
        .deletePost(postId, userId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))
    // .map(_.addCookie())
  }

  private val authRoutes = AuthedRoutes.of[LoginUser, F] {
    case GET -> Root as loginUser =>
      postService
        .getPosts
        .flatMap(Ok(_))
        .handleErrorWith(e => InternalServerError(e.toString()))

    case req @ POST -> Root / "add" as loginUser =>
      req
        .req
        .as[CreatePost]
        .flatMap { post =>
          postService
            .savePost(
              post.description,
              post.image,
              post.userId
            )
            .flatMap(Ok(_))
        }
        .handleErrorWith(e => InternalServerError(e.toString()))

    case DELETE -> Root / IntVar(userId) / IntVar(postId) as loginUser =>
      postService
        .deletePost(postId, userId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))

    case DELETE -> Root / "delete" :? UserId(userId) +& PostId(postId) as loginUser =>
      postService
        .deletePost(postId, userId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))

    // .map(_.addCookie())
  }

  val postRoutes = Router(
    prefix -> Auth0AuthorizationMiddleware(routes)
  )

  def routes(authMiddleware: AuthMiddleware[F, LoginUser]): HttpRoutes[F] = Router(
    prefix -> authMiddleware(authRoutes)
  )

}

object PostRoutes {
  def make[F[_]: Async](transactor: Transactor[F]): PostRoutes[F] = PostRoutes[F](PostService(transactor))
  def make[F[_]: Async](): PostRoutes[F] = PostRoutes[F](PostService(xa))
}
