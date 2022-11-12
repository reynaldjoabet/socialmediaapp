package db

import doobie._
import doobie.implicits._
import cats.effect.IO
import doobie.util.transactor
import cats.effect.kernel.Async

object Doobie {

  def xa[F[_]: Async] = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    "jdbc:postgresql:social_db",
    "postgres",
    "",
  )

  // A transactor that gets connections from java.sql.DriverManager and executes blocking operations
  val xa1 = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    "jdbc:postgresql:social_db", // connect URL (driver-specific)
    "postgres", // user
    "", // password
  )

}
