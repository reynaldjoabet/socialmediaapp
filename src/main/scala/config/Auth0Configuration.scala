package config

import cats.effect.kernel.Async
import cats.implicits._

import ciris.env
import lt.dvim.ciris.Hocon._

object Auth0Configuration {

  private val hocon = hoconAt("auth0")

  def auth0Config[F[_]: Async]: F[Auth0Configuration] =
    (
      env("AUTH0_DOMAIN").default("domain"),
      env("AUTH0_AUDIENCE").default("audience"),
      env("AUTH0_CLIENT_SECRET").default("clientSecret"),
      env("AUTH0_CLIENT_ID").default("clientId")
    ).parMapN((domain, audience, clientSecret, clientId) =>
        Auth0Configuration(domain, audience, clientSecret, clientId)
      )
      .load[F]

  def auth0Config2[F[_]: Async]: F[Auth0Configuration] =
    (
      hocon("domain").as[String],
      hocon("audience").as[String],
      hocon("clientSecret").as[String],
      hocon("clientId").as[String]
    ).parMapN((domain, audience, clientSecret, clientId) =>
        Auth0Configuration(domain, audience, clientSecret, clientId)
      )
      .load[F]

}

final case class Auth0Configuration private (
  domain: String,
  audience: String,
  clientSecret: String,
  clientId: String
)
