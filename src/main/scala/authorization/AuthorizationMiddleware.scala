package authorization

import cats.Applicative
import cats.data.Kleisli
import cats.data.OptionT
import org.http4s.Credentials.Token
import org.http4s.AuthScheme
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.headers.Authorization
import org.http4s.server.HttpMiddleware
import org.http4s.server.middleware.Caching
import scala.util.Failure
import scala.util.Success
import org.http4s.HttpRoutes
import org.http4s.headers.Cookie
import scala.util.Random

object AuthorizationMiddleware {

  // Status.Forbidden for authorisation failure
  // Status.Unauthorized for authentication failure
// A regex for parsing the Authorization header value
  // private val headerTokenRegex = """Bearer (.+?)""".r
  private def defaultAuthFailure[F[_]](
    implicit
    F: Applicative[F]
  ): Request[F] => F[Response[F]] = _ => F.pure(Response[F](Status.Forbidden))

  private def getBearerToken[F[_]](
    request: Request[F]
  ): Option[String] = request.headers.get[Authorization].collect {
    case Authorization(Token(AuthScheme.Bearer, token)) => token
  }

  // val cookie = Cookie("foo_session", domain = Some("localhost"), path=Some("/"))
  private def extractBearerToken[F[_]](
    request: Request[F]
  ): Option[String] = request.headers.get[Authorization].collect {
    case Authorization(Token(AuthScheme.OAuth, oauthToken)) => oauthToken
  }

  def apply[F[_]: Applicative]: HttpMiddleware[F] = authorize[F](defaultAuthFailure)

  def apply[F[_]: Applicative](http: HttpRoutes[F]) = authorize[F](defaultAuthFailure).apply(http)

  private def authorize[F[_]](
    onAuthFailure: Request[F] => F[Response[F]]
  )(
    implicit F: Applicative[F]
  ): HttpMiddleware[F] =
    service =>
      Kleisli { request: Request[F] =>
        getBearerToken(request) match {
          case Some(value) =>
            AuthorizationService.validateJwt(value) match {
              case Failure(exception) => OptionT.liftF(onAuthFailure(request))
              case Success(value)     => service(request)
            }
          case None => OptionT.liftF(onAuthFailure(request))
        }

      }

}
