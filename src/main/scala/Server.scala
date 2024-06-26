import cats.data.Kleisli
import cats.effect
import cats.effect._
import cats.implicits._

import com.comcast.ip4s._
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import config._
import db.DBMigration
import db.Doobie
import doobie.util.transactor._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder

object Server {

  private val ds = Http4sDsl[IO]
  import ds._

  val thePort   = port"9000"
  val theHost   = host"localhost"
  @inline val k = 9000

  /// val n=host"${k}"
  private val errorhandler: PartialFunction[Throwable, IO[Response[IO]]] = { case th: Throwable =>
    th.printStackTrace()
    std.Console[IO].error(s"InternalServerError: $th") *> InternalServerError(
      s"InternalServerError: $th"
    )
  }

  private def prometheusMeteredRoutes[F[_]: Async](
    transactor: Transactor[F]
  ) = HttpApi.make[F].prometheusMeteredRoutes(transactor)

  private val prometheusMeteredRoutes: Resource[IO, HttpRoutes[IO]] =
    HttpApi.make[IO].prometheusMeteredRoutes

  private val httpApp: Kleisli[IO, Request[IO], Response[IO]] = HttpApi.make[IO].middlewareHttpApp

  private val meteredApp = HttpApi.make[IO].meteredApp

  private def meteredApp[F[_]: Async](
    transactor: Transactor[F]
  ) = HttpApi.make[F].meteredApp(transactor)

  private def httpApp[F[_]: Async](
    transactor: Transactor[F]
  ) = HttpApi.make[F].middlewareHttpApp(transactor)

  def startServer: IO[Unit] =
    for {
      appConfig   <- AppConfiguration.appConfig[IO]
      serverConfig = appConfig.serverConfig
      auth0Config  = appConfig.auth0Config
      app         <- meteredApp

      csrfMiddleware <- HttpApi.csrfService[IO]
      // _ <- IO.println(s"${auth0Config.audience} and ${auth0Config.domain}")
      _ <- DBMigration.migrate()
      _ <- EmberServerBuilder
             .default[IO]
             .withErrorHandler(errorhandler)
             .withHttpApp(app)
             .withPort(Port.fromInt(serverConfig.port).get)
             .withHostOption(Host.fromString(serverConfig.host))
             // .withTLS()
             .build
             .useForever
             .race(
               IO.println("Press Any Key to stop the  server") *> IO
                 .readLine
                 .handleErrorWith(e => IO.println(s"There was an error! ${e.getMessage}")) *> IO
                 .println(
                   "Stopping Server"
                 ) *> DBMigration.reset()
             )
    } yield ()

  private def prometheusServer(httpRoutes: HttpRoutes[IO]): IO[Unit] =
    for {
      appConfig      <- AppConfiguration.appConfig[IO]
      serverConfig    = appConfig.serverConfig
      auth0Config     = appConfig.auth0Config
      csrfMiddleware <- HttpApi.csrfService[IO]
      // _ <- IO.println(s"${auth0Config.audience} and ${auth0Config.domain}")
      _ <- DBMigration.migrate()
      _ <- EmberServerBuilder
             .default[IO]
             .withErrorHandler(errorhandler)
             .withHttpApp(httpRoutes.orNotFound)
             .withPort(Port.fromInt(serverConfig.port).get)
             .withHost(Host.fromString(serverConfig.host).get)
             // .withTLS()
             .build
             .useForever
             .race(
               IO.println("Press Any Key to stop the  server") *> IO
                 .readLine
                 .handleErrorWith(e => IO.println(s"There was an error! ${e.getMessage}")) *> IO
                 .println(
                   "Stopping Server"
                 ) *> DBMigration.reset()
             )
    } yield ()

  private def server(transactor: Transactor[IO]): IO[Unit] =
    for {
      appConfig      <- AppConfiguration.appConfig[IO]
      serverConfig    = appConfig.serverConfig
      auth0Config     = appConfig.auth0Config
      app             = httpApp(transactor)
      csrfMiddleware <- HttpApi.csrfService[IO]
      // _ <- IO.println(s"${auth0Config.audience} and ${auth0Config.domain}")
      _ <- DBMigration.migrate()
      _ <- EmberServerBuilder
             .default[IO]
             .withErrorHandler(errorhandler)
             .withHttpApp(csrfMiddleware(app))
             .withPort(Port.fromInt(serverConfig.port).get)
             .withHost(Host.fromString(serverConfig.host).get)
             // .withTLS()
             .build
             .useForever
             .race(
               IO.println("Press Any Key to stop the  server") *> IO
                 .readLine
                 .handleErrorWith(e => IO.println(s"There was an error! ${e.getMessage}")) *> IO
                 .println(
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
