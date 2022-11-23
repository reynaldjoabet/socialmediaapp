package routes

import cats.effect.kernel.Async
import services._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import api._
import org.http4s._
import db.Doobie._
import authorization.AuthorizationMiddleware
import doobie.util.transactor._

final case class RelationshipRoutes[F[_]: Async](relationshipService: RelationshipService[F])
  extends Http4sDsl[F] {
  import org.http4s.server.Router

  private val prefix = "api/relationships"

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

object RelationshipRoutes {
  def make[F[_]: Async]() = RelationshipRoutes[F](RelationshipService(xa))

  def make[F[_]: Async](
    transactor: Transactor[F]
  ) = RelationshipRoutes[F](RelationshipService(transactor))

}
