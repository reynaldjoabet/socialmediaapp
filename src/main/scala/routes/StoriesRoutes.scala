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
import org.http4s.server.middleware.Metrics

final case class StoriesRoutes[F[_]: Async](storiesService: StoriesService[F])
  extends Http4sDsl[F] {
  import org.http4s.server.Router

  private val prefix = "api/stories"

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

object StoriesRoutes {
  def make[F[_]: Async]() = StoriesRoutes[F](StoriesService(xa))

  def make[F[_]: Async](transactor: Transactor[F]) = StoriesRoutes[F](StoriesService(transactor))
}
