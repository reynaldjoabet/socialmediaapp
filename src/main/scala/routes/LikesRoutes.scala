package routes

import org.http4s.dsl.Http4sDsl
import org.http4s._
import cats.effect.kernel.Async
import api._
import org.http4s.server.AuthMiddleware
import services._
import authorization.AuthorizationMiddleware
import db.Doobie._
import doobie.util.transactor._

final case class LikesRoutes[F[_]: Async](likesService: LikesService[F]) extends Http4sDsl[F] {

  import org.http4s.server.Router

  private val prefix = "api/likes"

  private val routes = HttpRoutes.of[F] { case GET -> Root / "hello" =>
    Ok("Hello dude") // .map(_.addCookie())
  }

  private val authRoutes = AuthedRoutes.of[LoginUser, F] {
    case GET -> Root / "hello" as loginUser => Ok("Hello dude") // .map(_.addCookie())
  }

  val likesRoutes = Router(
    prefix -> AuthorizationMiddleware(routes)
  )

  def routes(authMiddleware: AuthMiddleware[F, LoginUser]): HttpRoutes[F] = Router(
    prefix -> authMiddleware(authRoutes)
  )

}

object LikesRoutes {
  def make[F[_]: Async](transactor: Transactor[F]) = LikesRoutes[F](LikesService(transactor))
  def make[F[_]: Async]() = LikesRoutes[F](LikesService(xa))
}
