package config

import cats.effect.kernel.Async
import lt.dvim.ciris.Hocon._
import cats.implicits._
final case class FlywayConfiguration private (url: String, username: String, password: String)

object FlywayConfiguration {
  private val hocon = hoconAt("flyway")

  def flywayConfig[F[_]](
    implicit F: Async[F]
  ): F[FlywayConfiguration] =
    (
      hocon("url").as[String],
      hocon("username").as[String],
      hocon("password").as[String],
    ).parMapN(FlywayConfiguration.apply).load[F]

}
