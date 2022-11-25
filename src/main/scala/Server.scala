import org.http4s.ember.server.EmberServerBuilder

import cats.effect._
import cats.implicits._
import config._
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import db.DBMigration
import db.Doobie
import doobie.util.transactor._
import org.http4s.HttpRoutes

object Server {

  private def prometheusMeteredRoutes[F[_]: Async](
    transactor: Transactor[F]
  ) = HttpApi.make[F].prometheusMeteredRoutes(transactor)

  val prometheusMeteredRoutes = HttpApi.make[IO].prometheusMeteredRoutes
  private val httpApp = HttpApi.make[IO].middlewareHttpApp

  private val meteredApp = HttpApi.make[IO].meteredApp

  private def meteredApp[F[_]: Async](
    transactor: Transactor[F]
  ) = HttpApi.make[F].meteredApp(transactor)

  private def httpApp[F[_]: Async](
    transactor: Transactor[F]
  ) = HttpApi.make[F].middlewareHttpApp(transactor)

  def startServer: IO[Unit] =
    for {
      appConfig <- AppConfiguration.appConfig[IO]
      serverConfig = appConfig.serverConfig
      auth0Config = appConfig.auth0Config
      app <- meteredApp
      // _ <- IO.println(s"${auth0Config.audience} and ${auth0Config.domain}")
      _ <- DBMigration.migrate()
      _ <- EmberServerBuilder
        .default[IO]
        .withHttpApp(app)
        .withPort(Port.fromInt(serverConfig.port).get)
        .withHost(Host.fromString(serverConfig.host).get)
        // .withTLS()
        .build
        .useForever
        .race(
          IO.println("Press Any Key to stop the  server") *> IO
            .readLine
            .handleErrorWith(e => IO.println(s"There was an error! ${e.getMessage}")) *> IO.println(
            "Stopping Server"
          ) *> DBMigration.reset()
        )
    } yield ()

  def prometheusServer(httpRoutes: HttpRoutes[IO]): IO[Unit] =
    for {
      appConfig <- AppConfiguration.appConfig[IO]
      serverConfig = appConfig.serverConfig
      auth0Config = appConfig.auth0Config

      // _ <- IO.println(s"${auth0Config.audience} and ${auth0Config.domain}")
      _ <- DBMigration.migrate()
      _ <- EmberServerBuilder
        .default[IO]
        .withHttpApp(httpRoutes.orNotFound)
        .withPort(Port.fromInt(serverConfig.port).get)
        .withHost(Host.fromString(serverConfig.host).get)
        // .withTLS()
        .build
        .useForever
        .race(
          IO.println("Press Any Key to stop the  server") *> IO
            .readLine
            .handleErrorWith(e => IO.println(s"There was an error! ${e.getMessage}")) *> IO.println(
            "Stopping Server"
          ) *> DBMigration.reset()
        )
    } yield ()

  private def server(transactor: Transactor[IO]): IO[Unit] =
    for {
      appConfig <- AppConfiguration.appConfig[IO]
      serverConfig = appConfig.serverConfig
      auth0Config = appConfig.auth0Config
      app <- meteredApp(transactor)

      // _ <- IO.println(s"${auth0Config.audience} and ${auth0Config.domain}")
      _ <- DBMigration.migrate()
      _ <- EmberServerBuilder
        .default[IO]
        .withHttpApp(app)
        .withPort(Port.fromInt(serverConfig.port).get)
        .withHost(Host.fromString(serverConfig.host).get)
        // .withTLS()
        .build
        .useForever
        .race(
          IO.println("Press Any Key to stop the  server") *> IO
            .readLine
            .handleErrorWith(e => IO.println(s"There was an error! ${e.getMessage}")) *> IO.println(
            "Stopping Server"
          ) *> DBMigration.reset()
        )
    } yield ()

  def startServer1: IO[Unit] = Doobie.hikariTransactor[IO].use(transactor => server(transactor))

  def startPrometheus: IO[Unit] = prometheusMeteredRoutes.use(app => prometheusServer(app))

  def startPrometheus2: IO[Unit] = Doobie
    .hikariTransactor[IO]
    .use(transactor => prometheusMeteredRoutes(transactor).use(prometheusServer))

}
