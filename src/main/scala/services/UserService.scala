package services

import domain._
import cats.effect.kernel.Async
import doobie.util.transactor.Transactor
import doobie.implicits._

final case class UserService[F[_]: Async](private val xa: Transactor[F]) {

  def findUserByUsername(username: String): F[Option[User]] =
    sql"SELECT * FROM users WHERE username= $username"
      .query[User]
      .option
      .transact(xa)

  def saveUser(
    username: String,
    email: String,
    password: String,
    name: String,
    coverPicture: Option[String],
    profilePicture: Option[String],
    city: Option[String],
    website: Option[String]
  ): F[Int] =
    sql"INSERT INTO users (username,email,password,name,cover_picture,profile_picture,city,website) VALUES($username,$email,$password,$name,$coverPicture,$profilePicture,$city,$website)"
      .update
      .withUniqueGeneratedKeys[Int]("id")
      .transact(xa)

}
