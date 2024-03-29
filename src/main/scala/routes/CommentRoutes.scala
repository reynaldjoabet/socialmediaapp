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
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIString

final case class CommentRoutes[F[_]: Async](commentService: CommentService[F])
  extends Http4sDsl[F] {

  private object UserId extends QueryParamDecoderMatcher[Int]("userId")
  private object CommentId extends QueryParamDecoderMatcher[Int]("commentId")

  import org.http4s.server.Router

  private val prefix = "/api/comments"

  private val routes = HttpRoutes.of[F] {
    case GET -> Root / IntVar(postId) =>
      commentService
        .getComments(postId)
        .flatMap(Ok(_))
        .handleErrorWith(e => InternalServerError(e.toString()))
    case req @ POST -> Root / "add" =>
      req
        .as[CreateComment]
        .flatMap { comment =>
          commentService
            .saveComment(comment.description, comment.createdAt, comment.userId, comment.postId)
            .flatMap(Ok(_))
        }
        .handleErrorWith(e => InternalServerError(e.toString()))
    case DELETE -> Root / IntVar(userId) / IntVar(commentId) =>
      commentService
        .deleteComment(commentId, userId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))
    // .map(_.addCookie())
  }

  private val authRoutes = AuthedRoutes.of[LoginUser, F] {
    case GET -> Root / IntVar(postId) as loginUser =>
      commentService
        .getComments(postId)
        .flatMap(Ok(_))
        .handleErrorWith(e => InternalServerError(e.toString()))

    case req @ POST -> Root / "add" as loginUser =>
      req
        .req
        .as[CreateComment]
        .flatMap { comment =>
          commentService
            .saveComment(comment.description, comment.createdAt, comment.userId, comment.postId)
            .flatMap(Ok(_))
        }
        .handleErrorWith(e => InternalServerError(e.toString()))

    case DELETE -> Root / IntVar(userId) / IntVar(commentId) as loginUser =>
      commentService
        .deleteComment(commentId, userId)
        .flatMap(_ => Ok())
        .handleErrorWith(e => InternalServerError(e.toString()))

    case DELETE -> Root / "delete" :? UserId(userId) +& CommentId(commentId) as loginUser =>
      commentService
        .deleteComment(commentId, userId)
        .flatMap(_ => Ok())
        // .map(_.addHeader(Header.Raw(CIString("Content-Security-Policy"),"script-src 'none'")))
        .handleErrorWith(e => InternalServerError(e.toString()))

    // .map(_.addCookie())
  }

  val commentRoutes = Router(
    prefix -> Auth0AuthorizationMiddleware(routes)
  )

  def routes(authMiddleware: AuthMiddleware[F, LoginUser]): HttpRoutes[F] = Router(
    prefix -> authMiddleware(authRoutes)
  )

}

object CommentRoutes {

  def make[F[_]: Async](transactor: Transactor[F]): CommentRoutes[F] = CommentRoutes[F](
    CommentService(transactor)
  )

  def make[F[_]: Async]: CommentRoutes[F] = CommentRoutes[F](CommentService(xa))

}
