import cats.effect.ExitCode

import cats.effect.IO
import cats.effect.IOApp
import config.AppConfiguration
import fs2.Stream
import org.http4s.server.AuthMiddleware
import db.DBMigration
import cats.effect.std

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = Server.startServer().as(ExitCode.Success)

}
