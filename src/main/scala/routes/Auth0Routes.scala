package routes

import cats.effect.kernel.Async
import services._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import api._
import org.http4s._
import db.Doobie._
import authentication.Auth0AuthenticationMiddleware
import doobie.util.transactor._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
import cats.implicits._
import org.http4s.server.middleware.HttpsRedirect
import org.http4s.headers.`Content-Type`
import org.http4s.headers.Location

final case class Auth0Routes[F[_]: Async]() extends Http4sDsl[F] {
  import org.http4s.server.Router
  object CodeParam extends QueryParamDecoderMatcher[String]("code")

  private val routes = HttpRoutes.of[F] {

    case GET -> Root / authorize => {
      val headers = Headers(
        Location(Uri.unsafeFromString("https://")),
        `Content-Type`(MediaType.text.xml)
      )
      Async[F].pure(Response[F](status = TemporaryRedirect, headers = headers))
    }

    case GET -> Root / oauth / token => TemporaryRedirect()

  }

  val auth0Routes = Router(
    "/" -> Auth0AuthenticationMiddleware(routes)
  )

}
