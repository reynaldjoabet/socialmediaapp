package routes

import org.http4s.dsl.Http4sDsl
import cats.effect.kernel.Async
import org.http4s.server.AuthMiddleware
import api._
import org.http4s._
import authorization.AuthorizationMiddleware
import services._
import db.Doobie._
import doobie.util.transactor._

final case class CommentRoutes[F[_]: Async](commetService: CommentService[F]) extends Http4sDsl[F] {

  case class Foo(i: Int)
  implicit val fooDecoder = QueryParamDecoder.intQueryParamDecoder.map(Foo(_))

  object FooMatcher extends QueryParamDecoderMatcher[Foo]("foo")

  import org.http4s.server.Router

  private val prefix = "api/comments"

  private val routes = HttpRoutes.of[F] { case GET -> Root / "hello" =>
    Ok("Hello dude") // .map(_.addCookie())
  }

  private val authRoutes = AuthedRoutes.of[LoginUser, F] {
    case GET -> Root / "hello" as loginUser => Ok("Hello dude") // .map(_.addCookie())
  }

  val commentRoutes = Router(
    prefix -> AuthorizationMiddleware(routes)
  )

  def routes(authMiddleware: AuthMiddleware[F, LoginUser]): HttpRoutes[F] = Router(
    prefix -> authMiddleware(authRoutes)
  )

}

object CommentRoutes {
  def make[F[_]: Async](transactor: Transactor[F]) = CommentRoutes[F](CommentService(transactor))
  def make[F[_]: Async] = CommentRoutes[F](CommentService(xa))

}
