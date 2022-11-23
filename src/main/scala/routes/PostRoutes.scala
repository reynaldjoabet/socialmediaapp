package routes

import org.http4s.dsl.Http4sDsl
import org.http4s._
import cats.effect.kernel.Async
import org.http4s.server.AuthMiddleware
import services._
import api._
import authorization.AuthorizationMiddleware
import db.Doobie._
import doobie.util.transactor._

final case class PostRoutes[F[_]: Async](postService: PostService[F]) extends Http4sDsl[F] {

  import org.http4s.server.Router

  private val prefix = "api/posts"

  private val routes = HttpRoutes.of[F] { case GET -> Root / "hello" =>
    Ok("Hello dude") // .map(_.addCookie())
  }

  private val authRoutes = AuthedRoutes.of[LoginUser, F] {
    case GET -> Root / "hello" as loginUser => Ok("Hello dude") // .map(_.addCookie())
  }

  val postRoutes = Router(
    prefix -> AuthorizationMiddleware(routes)
  )

  def routes(authMiddleware: AuthMiddleware[F, LoginUser]): HttpRoutes[F] = Router(
    prefix -> authMiddleware(authRoutes)
  )

}

object PostRoutes {
  def make[F[_]: Async](transactor: Transactor[F]) = PostRoutes[F](PostService(transactor))
  def make[F[_]: Async]() = PostRoutes[F](PostService(xa))
}
