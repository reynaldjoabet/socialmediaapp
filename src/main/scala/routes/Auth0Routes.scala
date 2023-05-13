package routes

import cats.effect.kernel.Async
import services._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import api._
import org.http4s._
import db.Doobie._
import authorization.Auth0AuthorizationMiddleware
import doobie.util.transactor._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
import cats.implicits._
import org.http4s.server.middleware.HttpsRedirect
import org.http4s.headers.`Content-Type`
import org.http4s.headers.Location

import cats.effect._
import cats.effect.std.Dispatcher

import cats.implicits._
import fs2.{Chunk, Stream}

import io.circe.Json
import org.http4s.circe.jsonOf
//import org.http4s._
//import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import scala.concurrent.ExecutionContext.Implicits.global

import org.http4s.server.AuthMiddleware
import org.http4s._

final case class Auth0Routes[F[_]: Async](authoService: Auth0Service[F]) extends Http4sDsl[F] {
  import org.http4s.server.Router
//object CodeParamp extends QueryParamDecoderMatcher[String]("code")
  object CodeParam extends OptionalQueryParamDecoderMatcher[String]("code")

  private val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> Root / "token" :? CodeParam(authorizationCode) =>
      req.cookies.find(_.name == "")
      authorizationCode match {
        case None => Response(Status.Unauthorized).pure[F]
        case Some(code) =>
          val client = JavaNetClientBuilder[F].create
          val clientId = "NMXhdvC1"
          val clientSecret = "bRLP2YxR"

          (for {

            accessToken <- authoService.fetchBearerToken(client, clientId, clientSecret, code)

            data <- authoService.fetchDataFromApiJson(client, accessToken)
          } yield data)
            .flatMap(Ok(_))
      }

  }

  val auth0Routes = Router("api" -> routes)
}

object Auth0Routes {
  def make[F[_]: Async](transactor: Transactor[F]): Auth0Routes[F] = Auth0Routes[F](Auth0Service())
}
