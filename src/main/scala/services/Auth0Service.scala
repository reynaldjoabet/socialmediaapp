package services
import cats.effect.kernel.Async
import org.http4s.client.Client
import domain._
import org.http4s.client._
import org.http4s._
import  cats.implicits._

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

final case class Auth0Service[F[_]: Async]() extends Http4sClientDsl[F]{
    def fetchBearerToken(client: Client[F], clientId: String, clientSecret: String, authorizationCode: String,domain:String): F[String] = {
        val request = Method.POST(
          UrlForm(
            "client_id" -> clientId,
            "client_secret" -> clientSecret,
            "code" -> authorizationCode,
            "grant_type" -> "authorization_code"),
          Uri.unsafeFromString(s"https://$domain/oauth/token"))
        client.expect(request)(jsonOf[F, String])
      }

      def requestAuthCode(clientId: String, localPort: Int,domain:String): F[Unit] = {
          Async[F].blocking {
            import scala.sys.process._
            s"open http://$domain/oauth/authorize?client_id=${clientId}&response_type=code&redirect_uri=http://localhost:${localPort}&scope=read".!

          }
          }
}
