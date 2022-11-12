package config

import cats.implicits._
import ciris.env
import ciris.ConfigValue
import cats.effect.IO
import cats.effect.kernel.Async

import lt.dvim.ciris.Hocon._
import cats.implicits._

object Auth0Configuration {
  private val hocon = hoconAt("auth0")

  def auth0Config[F[_]: Async]: F[Auth0Configuration] =
    (
      env("AUTH0_DOMAIN").default("domain"),
      env("AUTH0_AUDIENCE").default("audience"),
    ).parMapN((domain, audience) => Auth0Configuration(domain, audience)).load[F]

  def auth0Config2[F[_]: Async]: F[Auth0Configuration] =
    (
      hocon("AUTH0_DOMAIN").as[String],
      hocon("AUTH0_AUDIENCE").as[String],
    ).parMapN((domain, audience) => Auth0Configuration(domain, audience)).load[F]

}

final case class Auth0Configuration private (domain: String, audience: String)
