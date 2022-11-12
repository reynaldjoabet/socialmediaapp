import cats.data.Kleisli

import org.http4s.ember.server.EmberServerBuilder
import cats.effect._
import cats.implicits._
import config._
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import org.http4s.server.Server
import db.DBMigration

object Server {

  private val httpApp = HttpApi.make[IO].middlewareHttpApp

  def startServer(): IO[Unit] =
    for {
      appConfig <- AppConfiguration.appConfig[IO]
      serverConfig = appConfig.serverConfig
      auth0Config = appConfig.auth0Config

      // _ <- IO.println(s"${auth0Config.audience} and ${auth0Config.domain}")
      _ <- DBMigration.migrate()
      fiber <- EmberServerBuilder
        .default[IO]
        .withHttpApp(httpApp)
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

}
