package config

import cats.effect.kernel.Async

//import lt.dvim.ciris.Hocon._
import cats.implicits._

final case class AppConfiguration private (
  flyway: FlywayConfiguration,
  serverConfig: ServerConfiguration,
  auth0Config: Auth0Configuration,
)

object AppConfiguration {

  def appConfig[F[_]: Async]: F[AppConfiguration] =
    for {
      serverConfig <- ServerConfiguration.serverConfig[F]
      flywayConfig <- FlywayConfiguration.flywayConfig[F]
      auth0Config <- Auth0Configuration.auth0Config[F]
    } yield AppConfiguration(flywayConfig, serverConfig, auth0Config)

}
