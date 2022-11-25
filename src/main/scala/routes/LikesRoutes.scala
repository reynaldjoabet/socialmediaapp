package routes

import org.http4s.dsl.Http4sDsl
import org.http4s._
import cats.effect.kernel.Async
import api._
import org.http4s.server.AuthMiddleware
import services._
import authentication.AuthenticationMiddleware
import db.Doobie._
import doobie.util.transactor._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
import cats.implicits._

final case class LikesRoutes[F[_]: Async](likesService: LikesService[F]) extends Http4sDsl[F] {

  import org.http4s.server.Router
  private object UserId extends QueryParamDecoderMatcher[Int]("userId")
  private object PostId extends QueryParamDecoderMatcher[Int]("postId")
  private val prefix = "api/likes"

  private val routes = HttpRoutes.of[F] {
    case GET -> Root / IntVar(postId) =>
      likesService
        .getLikes(postId)
        .flatMap(Ok(_))
        .handleErrorWith(e => InternalServerError(e.toString()))
    case req @ POST -> Root / "add" =>
      req
        .as[CreateLike]
        .flatMap { like =>
          likesService
            .saveLikes(like.userId, like.postId)
            .flatMap(Ok(_))
        }
        .handleErrorWith(e => InternalServerError(e.toString()))
    case DELETE -> Root / IntVar(userId) / IntVar(postId) =>
      likesService
        .deleteLikes(userId, postId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))
    // .map(_.addCookie())
  }

  private val authRoutes = AuthedRoutes.of[LoginUser, F] {
    case GET -> Root / IntVar(postId) as loginUser =>
      likesService
        .getLikes(postId)
        .flatMap(Ok(_))
        .handleErrorWith(e => InternalServerError(e.toString()))

    case req @ POST -> Root / "add" as loginUser =>
      req
        .req
        .as[CreateLike]
        .flatMap { like =>
          likesService
            .saveLikes(
              like.userId,
              like.postId,
            )
            .flatMap(Ok(_))
        }
        .handleErrorWith(e => InternalServerError(e.toString()))

    case DELETE -> Root / IntVar(userId) / IntVar(postId) as loginUser =>
      likesService
        .deleteLikes(userId, postId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))

    case DELETE -> Root / "delete" :? UserId(userId) +& PostId(postId) as loginUser =>
      likesService
        .deleteLikes(userId, postId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))

    // .map(_.addCookie())
  }

  val likesRoutes = Router(
    prefix -> AuthenticationMiddleware(routes)
  )

  def routes(authMiddleware: AuthMiddleware[F, LoginUser]): HttpRoutes[F] = Router(
    prefix -> authMiddleware(authRoutes)
  )

}

object LikesRoutes {
  def make[F[_]: Async](transactor: Transactor[F]) = LikesRoutes[F](LikesService(transactor))
  def make[F[_]: Async]() = LikesRoutes[F](LikesService(xa))
}
