import cats.effect.kernel.Async

import services.UserService
import cats.data.Kleisli
import authorization._
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.server.Server
import org.http4s.server.middleware._
import scala.concurrent.duration._
import cats.effect.kernel.Async
import routes._

import org.http4s.server.HttpMiddleware

import db.DBMigration

import org.http4s.Method._

case class HttpApi[F[_]: Async]() {

  // private  val middleware= AuthorizationMiddleware

  private val corsConfig = CORSConfig
    .default
    .withAnyOrigin(false)
    .withAllowCredentials(false)
    .withAllowedMethods(Some(Set(POST, PUT, GET, DELETE)))
    .withAllowedOrigins(Set("http://localhost:3000"))

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = { http: HttpRoutes[F] =>
    AutoSlash(http)
  } andThen { http: HttpRoutes[F] =>
    CORS(http, corsConfig)
  } andThen { http: HttpRoutes[F] =>
    Timeout(60.seconds)(http)
  }

  private val loggers: HttpApp[F] => HttpApp[F] = { http: HttpApp[F] =>
    RequestLogger.httpApp(true, true)(http)
  } andThen { http: HttpApp[F] =>
    ResponseLogger.httpApp(true, true)(http)
  }

  private val httpRoutes =
    HealthRoutes[F]().router <+> UserRoutes.make[F].router <+> LogoutRoutes().logoutRoutes <+>
      CommentRoutes.make[F].commentRoutes <+> PostRoutes.make[F].postRoutes <+> LikesRoutes
        .make[F]
        .likesRoutes

  val middlewareHttpApp = middleware(httpRoutes).orNotFound
}

object HttpApi {
  def make[F[_]: Async]() = HttpApi()
}
