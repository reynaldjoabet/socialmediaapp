
package authorization
import cats.Applicative
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import cats.effect.kernel.Async
import org.http4s.Credentials.Token
import org.http4s.AuthScheme
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.headers.Authorization
import org.http4s.server.HttpMiddleware
import pdi.jwt.JwtClaim

import scala.util.Failure
import scala.util.Success
import org.http4s.HttpRoutes
import org.http4s.server.AuthMiddleware
import org.http4s.ContextRequest
import org.http4s.AuthedRoutes

object Auth0AuthorizationMiddleware {

  // Status.Forbidden for authorisation failure
  // Status.Unauthorized for authentication failure
// A regex for parsing the Authorization header value
  // private val headerTokenRegex = """Bearer (.+?)""".r

  /*
  401 Unauthorized is the status code to return when the client provides no credentials or invalid credentials. 
  403 Forbidden is the status code to return when a client has valid credentials 
  but not enough privileges to perform an action on a resource
  */
  private def defaultAuthFailure[F[_]](
    implicit
    F: Applicative[F]
  ): Request[F] => F[Response[F]] = _ => F.pure(Response[F](Status.Unauthorized))

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

  def apply[F[_]: Async](
    httpRoutes: HttpRoutes[F]
  ): HttpRoutes[F] = authorizeRoutes[F](defaultAuthFailure).apply(httpRoutes)

  def authorizeAuthedRoutes[T, F[_]: Async](
    httpRoutes: AuthedRoutes[T, F]
  ): HttpRoutes[F] = authorizeAuthedRoutes(defaultAuthFailure).apply(httpRoutes)

  private def authorizeRoutes[F[_]: Async](
    onAuthFailure: Request[F] => F[Response[F]]
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

  private def authorizeAuthedRoutes[T, F[_]: Async](
    onAuthFailure: Request[F] => F[Response[F]]
  ): AuthMiddleware[F, T] =
    service =>
      Kleisli { request: Request[F] =>
        getBearerToken(request) match {
          case Some(value) =>
          AuthorizationService.validateJwt(value) match {
              case Failure(exception) => OptionT.liftF(onAuthFailure(request))
              case Success(claim)     => service(ContextRequest(claim.asInstanceOf[T], request))

            }
          case None => OptionT.liftF(onAuthFailure(request))
        }

      }

}
