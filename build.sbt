ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

val http4sVersion = "0.23.16"
val cirisVersion = "2.4.0"
val circeVersion = "0.14.3"
val flywayVersion = "8.5.12"
val bcryptVersion = "0.4.1"
val postgresVersion = "42.3.6"
val doobieVersion = "1.0.0-RC1"
val quillVersion = "4.6.0"
val catsEffectVersion = "3.3.12"
val fs2Version = "3.3.0"
def circe(artifact: String): ModuleID = "io.circe" %% s"circe-$artifact" % circeVersion
def ciris(artifact: String): ModuleID = "is.cir" %% artifact % cirisVersion
def http4s(artifact: String): ModuleID = "org.http4s" %% s"http4s-$artifact" % http4sVersion

val circeCore = circe("core")
val circeGeneric = circe("generic")

val cirisCore = ciris("ciris")
val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
val fs2 = "co.fs2" %% "fs2-core" % fs2Version
///val quill="io.getquill" %% "quill-doobie" % quillVersion

val http4sDsl = http4s("dsl")
val http4sServer = http4s("ember-server")
val http4sClient = http4s("ember-client")
val http4sCirce = http4s("circe")
val jwt = "com.github.jwt-scala" %% "jwt-circe" % "9.1.1"
val jwks = "com.auth0" % "jwks-rsa" % "0.21.2"
val logbackVersion = "1.4.4"
val postgres = "org.postgresql" % "postgresql" % postgresVersion
val flyway = "org.flywaydb" % "flyway-core" % flywayVersion
val ciris_hocon = "lt.dvim.ciris-hocon" %% "ciris-hocon" % "1.0.1"

val doobie = "org.tpolecat" %% "doobie-core" % doobieVersion

val doobie_postgres =
  "org.tpolecat" %% "doobie-postgres" % doobieVersion // Postgres driver 42.3.1 + type mappings.
val logback = "ch.qos.logback" % "logback-classic" % logbackVersion
val bcrypt = "de.svenkubiak" % "jBCrypt" % bcryptVersion

lazy val root = (project in file("."))
  .settings(
    name := "SocialMediaApp",
    libraryDependencies ++= Seq(
      cirisCore,
      http4sDsl,
      http4sServer,
      http4sClient,
      flyway,
      doobie,
      doobie_postgres,
      http4sCirce,
      circeCore,
      circeGeneric,
      jwt,
      jwks,
      postgres,
      ciris_hocon,
      logback,
      bcrypt,
      catsEffect,
      fs2,
    ),
  )
