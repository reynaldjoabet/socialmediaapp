package db

import doobie._
import doobie.implicits._
import cats.effect.IO
import doobie.util.transactor
import cats.effect.kernel.{Async, Resource}
import doobie.hikari.HikariTransactor
import doobie.hikari.HikariTransactor.newHikariTransactor
import doobie.util.transactor.Transactor.Aux

import scala.concurrent.ExecutionContext

object Doobie {

  private val ec = ExecutionContext.global

  def xa[F[_]: Async]: Aux[F, Unit] = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    "jdbc:postgresql:social_db",
    "postgres",
    "",
  )

//HikariTransactor
  def hikariTransactor[F[_]: Async]: Resource[F, HikariTransactor[F]] = newHikariTransactor[F](
    "org.postgresql.Driver",
    "jdbc:postgresql:social_db",
    "postgres",
    "",
    ec,
  )

  // A transactor that gets connections from java.sql.DriverManager and executes blocking operations
  val xa1: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    "jdbc:postgresql:social_db", // connect URL (driver-specific)
    "postgres", // user
    "", // password
  )

}
