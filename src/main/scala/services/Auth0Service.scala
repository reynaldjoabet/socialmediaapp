package services

import cats.effect.kernel.Async
import org.http4s.client.Client
import domain._
import org.http4s.client._
import org.http4s._
import cats.implicits._
import org.http4s.client.middleware.RequestLogger
import org.http4s.client.middleware.ResponseLogger
import io.circe.Json
import org.http4s.circe.jsonOf
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.client.dsl.Http4sClientDsl

final case class Auth0Service[F[_]: Async]() extends Http4sClientDsl[F] {

  def fetchBearerToken(
    client: Client[F],
    clientId: String,
    clientSecret: String,
    authorizationCode: String
  ): F[String] = {

    val request = Method.POST(
      UrlForm(
        "client_id" -> clientId,
        "client_secret" -> clientSecret,
        "code" -> authorizationCode,
        "grant_type" -> "authorization_code",
        "redirect_uri" -> "http://localhost:3000/challenges"
      ),
      Uri.uri("https://bamenda21.us.auth0.com/oauth/token")
    )
    RequestLogger[F](true, true)(client).expect(request)(jsonOf[F, String])
  }

  def fetchDataFromApiJson(client: Client[F], bearerToken: String): F[Vector[Json]] = {
    val request = Method
      .GET
      .apply(
        Uri.unsafeFromString(s"http://localhost:8090/api/relationships"),
        Accept(MediaType.application.json),
        Authorization(Credentials.Token(AuthScheme.Bearer, bearerToken))
      )

    client.expect(request)(jsonOf[F, Vector[Json]]).flatTap(_ => (println(request)).pure[F])
  }

}
