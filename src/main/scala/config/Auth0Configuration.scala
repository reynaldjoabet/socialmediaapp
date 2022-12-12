package config

import cats.implicits._
import ciris.env

import cats.effect.kernel.Async

import lt.dvim.ciris.Hocon._


object Auth0Configuration {
  private val hocon = hoconAt("auth0")

  def auth0Config[F[_]: Async]: F[Auth0Configuration] =
    (
      env("AUTH0_DOMAIN").default("domain"),
      env("AUTH0_AUDIENCE").default("audience")
    ).parMapN((domain, audience) => Auth0Configuration(domain, audience)).load[F]

  def auth0Config2[F[_]: Async]: F[Auth0Configuration] =
    (
      hocon("domain").as[String],
      hocon("audience").as[String]
    ).parMapN((domain, audience) => Auth0Configuration(domain, audience)).load[F]

}

final case class Auth0Configuration private (domain: String, audience: String)
