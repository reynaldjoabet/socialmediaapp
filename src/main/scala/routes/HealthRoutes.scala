package routes

import cats.effect.IO
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import cats.effect.kernel.Async

case class HealthRoutes[F[_]: Async]() extends Http4sDsl[F] {

  import org.http4s.server.Router

  private val prefix = ""

  private val routes = HttpRoutes.of[F] { case GET -> Root / "health" =>
    Ok("app works just fine") // .map(_.addCookie())
  }

  val router: HttpRoutes[F] = Router(prefix -> routes)
}
