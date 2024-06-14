package config

import cats.effect.kernel.Async
import cats.effect.IO
///import model._
import cats.implicits._

import lt.dvim.ciris.Hocon._

final case class ServerConfiguration private (host: String, port: Int)

object ServerConfiguration {

  private val hocon = hoconAt("server")

  def serverConfig[F[_]](implicit
    ev: Async[F]
  ): F[ServerConfiguration] =
    (
      hocon("port").as[Int],
      hocon("host").as[String]
    ).parMapN((port, host) => ServerConfiguration(host, port)).load[F]

}
