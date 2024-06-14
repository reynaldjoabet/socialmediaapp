package routes

import cats.effect.kernel.Async
import cats.implicits._

import api._
import authorization.Auth0AuthorizationMiddleware
import doobie.util.transactor._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware

final case class LogoutRoutes[F[_]: Async]() extends Http4sDsl[F] {

  import org.http4s.server.Router

  private val prefix = "/api/auth"

  private val routes = HttpRoutes.of[F] { case GET -> Root / "logout" =>
    NoContent().map(_.removeCookie("sessionID"))
  }

  private val authRoutes = AuthedRoutes.of[LoginUser, F] {
    case req @ GET -> Root / "logout" as loginUser => NoContent().map(_.removeCookie("sessionID"))
  }

  val logoutRoutes = Router(
    prefix -> Auth0AuthorizationMiddleware(routes)
  )

  def routes(authMiddleware: AuthMiddleware[F, LoginUser]): HttpRoutes[F] = Router(
    prefix -> authMiddleware(authRoutes)
  )

}
