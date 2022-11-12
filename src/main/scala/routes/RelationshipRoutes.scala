package routes

final case class RelationshipRoutes()

object RelationshipRoutes {
  def make[F[_]: Async]() = RelationshipRoutes[F](RelationshipService(xa))
}
