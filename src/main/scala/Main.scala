import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = Server.startServer1.as(ExitCode.Success) // Server.startServer().as(ExitCode.Success)

}
