
package authorization
import com.auth0.jwk.UrlJwkProvider
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtBase64
import pdi.jwt.JwtClaim
import pdi.jwt.JwtCirce
import com.auth0.jwk.JwkException

import java.time.Clock
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import cats.effect.IO
import pdi.jwt.JwtHeader
import cats.effect.kernel.Async
import com.auth0.jwk.Jwk

object AuthorizationService {

  private implicit val clock = Clock.systemUTC()
  // A regex that defines the JWT pattern and allows us to
  // extract the header, claims and signature
  private val jwtRegex = """(.+?)\.(.+?)\.(.+?)""".r

  /** // Your Auth0 domain, read from configuration private def domain =
    * config.get[String]("auth0.domain")
    *
    * // Your Auth0 audience, read from configuration private def audience =
    * config.get[String]("auth0.audience")
    */

  private val domain = System.getenv("AUTH0_DOMAIN")
  private val audience = System.getenv("AUTH0_AUDIENCE")

  /** // The issuer of the token. For Auth0, this is just your Auth0 // domain including the URI
    * scheme and a trailing slash. private def issuer = s"https://$domain/"
    */
  private val issuer = s"https://$domain/"

  // Validates a JWT and potentially returns the claims if the token was
  // successfully parsed and validated
  def validateJwt(token: String): Try[JwtClaim] =
    for {
      jwk <- getJwk(token) // Get the secret key for this token
      claims <- JwtCirce.decode(
        token,
        jwk.getPublicKey,
        Seq(JwtAlgorithm.RS256)
      ) // Decode the token using the secret key
      k=println(claims.issuer)
      _ <- validateClaims(claims) // validate the data stored inside the token
    } yield claims

  def validateJwt2(token: String): IO[JwtClaim] =
    for {
      jwk <- IO.fromTry(getJwk(token)) // Get the secret key for this token
      claims <- IO.fromTry(
        JwtCirce.decode(
          token,
          jwk.getPublicKey,
          Seq(JwtAlgorithm.RS256)
        )
      ) // Decode the token using the secret key
      _ <- IO.fromTry(validateClaims(claims)) // validate the data stored inside the token
    } yield claims

  // Splits a JWT into it's 3 component parts
  private val splitToken: String => Try[(String, String, String)] =
    (jwt: String) =>
      jwt match {
        case jwtRegex(header, body, sig) => Success((header, body, sig))
        case _ => Failure(new Exception("Token does not match the correct pattern"))
      }

  // As the header and claims data are base64-encoded, this function
  // decodes those elements
  private val decodeElements: Try[(String, String, String)] => Try[(String, String, String)] =
    (data: Try[(String, String, String)]) =>
      data map { case (header, body, sig) =>
        (JwtBase64.decodeString(header), JwtBase64.decodeString(body), sig)
      }

  // Gets the JWK from the JWKS endpoint using the jwks-rsa library
  private def getJwk(token: String): Try[Jwk] =
    (splitToken andThen decodeElements)(token) flatMap { case (header, _, _) =>
      val jwtHeader: JwtHeader = JwtCirce.parseHeader(header) // extract the header
      val jwkProvider = new UrlJwkProvider(s"https://$domain")

      // Use jwkProvider to load the JWKS data and return the JWK
      jwtHeader.keyId.map { k =>
        Try(jwkProvider.get(k))
      } getOrElse Failure(new JwkException("Unable to retrieve kid"))
    }

  private def getJwk[F[_]](
    token: String,
    domain: String
  )(
    implicit F: Async[F]
  ): F[Jwk] = F.fromTry {
    (splitToken andThen decodeElements)(token) flatMap { case (header, _, _) =>
      val jwtHeader: JwtHeader = JwtCirce.parseHeader(header) // extract the header
      val jwkProvider = new UrlJwkProvider(s"https://$domain")

      // Use jwkProvider to load the JWKS data and return the JWK
      jwtHeader.keyId.map { k =>
        Try(jwkProvider.get(k))
      } getOrElse Failure(new JwkException("Unable to retrieve kid"))
    }
  }

  // Validates the claims inside the token. 'isValid' checks the issuedAt, expiresAt,
  // issuer and audience fields.
  private val validateClaims =
    (claims: JwtClaim) =>
      if (claims.isValid(issuer, audience)) {
        println(claims.toJson)
        Success(claims)
      } else {
        Failure(new Exception("The JWT did not pass validation"))
      }

  private def validateClaims[F[_]: Async](
    claims: JwtClaim,
    issuer: String,
    audience: String
  ): F[JwtClaim] = Async[F].fromTry(if (claims.isValid(issuer, audience)) {
    Success(claims)
  } else {
    Failure(new Exception("The JWT did not pass validation"))
  })

}
