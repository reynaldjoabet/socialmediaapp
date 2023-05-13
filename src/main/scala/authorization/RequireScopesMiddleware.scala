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
import io.circe.Json
import io.circe.parser._
import scala.util.Failure
import scala.util.Success
import org.http4s.HttpRoutes
import org.http4s.server.AuthMiddleware
import org.http4s.ContextRequest
import org.http4s.AuthedRoutes
import org.http4s.Credentials
import io.circe.ParsingFailure
import cats.implicits._

object RequireScopesMiddleware {

  private def defaultAuthFailure[F[_]](
    implicit
    F: Applicative[F]
  ): Request[F] => F[Response[F]] = _ => F.pure(Response[F](Status.Unauthorized))

  private def getBearerToken[F[_]](
    request: Request[F]
  ): Option[String] = request.headers.get[Authorization].collect {
    case Authorization(Credentials.Token(AuthScheme.Bearer, token)) => token
  }

  def apply[F[_]: Async](
    httpRoutes: HttpRoutes[F],
    requiredScopes: Set[String]
  ): HttpRoutes[F] = authorizeRoutes[F](defaultAuthFailure, requiredScopes).apply(httpRoutes)

  private def authorizeRoutes[F[_]: Async](
    onAuthFailure: Request[F] => F[Response[F]],
    requiredScopes: Set[String]
  ): HttpMiddleware[F] =
    service =>
      Kleisli { request: Request[F] =>
        getBearerToken(request) match {
          case Some(token) =>
            AuthorizationService.validateJwt(token) match {
              case Failure(exception) => OptionT.liftF(onAuthFailure(request))
              case Success(claim) => checkScopes(claim.content, requiredScopes, service, request)

            }

          case None => OptionT.liftF(onAuthFailure(request))
        }

      }

  private def checkScopes[F[_]: Async](
    content: String,
    requiredScopes: Set[String],
    service: HttpRoutes[F],
    request: Request[F]
  ): OptionT[F, Response[F]] = {
    parse(content) match {
      case Left(error) => OptionT.liftF((Response[F](Status.Forbidden)).pure[F])
      case Right(jsonValue) =>
        val providedScopes: Set[String] =
          jsonValue
            .findAllByKey("scope")
            .headOption // need to handle scenario where permissions is not present
            .map(
              _.toString()
                .replaceAll("\"", "")
                .split(" ")
            )
            .getOrElse(Array.empty[String])
            .toSet
        if (requiredScopes.subsetOf(providedScopes))
          service(request)
        else
          OptionT.liftF((Response[F](Status.Forbidden)).pure[F])
    }

  }

}
