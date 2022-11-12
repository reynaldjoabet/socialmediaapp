package config

///import model._
import cats.implicits._
import lt.dvim.ciris.Hocon._
import cats.effect.IO
import cats.effect.kernel.Async
final case class ServerConfiguration private (host: String, port: Int)

object ServerConfiguration {
  private val hocon = hoconAt("server")

  def serverConfig[F[_]](implicit ev: Async[F]): F[ServerConfiguration] =
    (
      hocon("port").as[Int],
      hocon("host").as[String],
    ).parMapN((port, host) => ServerConfiguration(host, port)).load[F]

}
