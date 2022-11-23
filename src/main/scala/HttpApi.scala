import cats.effect.Clock

import cats.effect.MonadCancel
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import com.codahale.metrics.MetricFilter
import com.codahale.metrics.graphite.Graphite
import com.codahale.metrics.graphite.GraphiteReporter

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import cats.implicits._
import org.http4s._
import org.http4s.metrics.prometheus.Prometheus
import org.http4s.metrics.prometheus.PrometheusExportService
import org.http4s.server.middleware._

import scala.concurrent.duration._
import com.codahale.metrics.MetricRegistry
import routes._
import com.codahale.metrics.SharedMetricRegistries
import doobie.util.transactor._
import io.prometheus.client.CollectorRegistry
import org.http4s.Method._
import org.http4s.metrics.dropwizard.Dropwizard
import org.http4s.metrics.dropwizard.metricsService

class HttpApi[F[_]](implicit F: Async[F]) {

  // private  val middleware= AuthorizationMiddleware

  private val corsConfig = CORSConfig
    .default
    .withAnyOrigin(false)
    .withAllowCredentials(false)
    .withAllowedMethods(Some(Set(POST, PUT, GET, DELETE)))
    .withAllowedOrigins(Set("http://localhost:3000"))

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = { http: HttpRoutes[F] =>
    AutoSlash(http)
  } andThen { http: HttpRoutes[F] =>
    Timeout(60.seconds)(http)
  }

  private val loggers: HttpApp[F] => HttpApp[F] = { http: HttpApp[F] =>
    RequestLogger.httpApp(true, true)(http)
  } andThen { http: HttpApp[F] =>
    ResponseLogger.httpApp(true, true)(http)
  }

  private val httpRoutes: HttpRoutes[F] =
    HealthRoutes[F].router <+> UserRoutes.make[F].router <+> LogoutRoutes[F].logoutRoutes <+>
      CommentRoutes.make[F].commentRoutes <+> PostRoutes.make[F].postRoutes <+> LikesRoutes
        .make[F]
        .likesRoutes

  private def httpRoutes(transactor: Transactor[F]): HttpRoutes[F] =
    HealthRoutes[F].router <+> UserRoutes.make[F].router <+> LogoutRoutes[F].logoutRoutes <+>
      CommentRoutes.make[F](transactor).commentRoutes <+> PostRoutes
        .make[F](transactor)
        .postRoutes <+> LikesRoutes
        .make[F](transactor)
        .likesRoutes

  def middlewareHttpApp(transactor: Transactor[F]) = middleware(httpRoutes(transactor)).orNotFound

  val middlewareHttpApp = middleware(httpRoutes).orNotFound

  val meteredApp =
    for {
      registry <- F.delay(new MetricRegistry)
      meteredRoutes = Metrics[F](Dropwizard(registry, "server"))(middleware(httpRoutes))
      _ <- graphiteReporter(registry)
    } yield (meteredRoutes <+> metricsService(registry)).orNotFound

  def meteredApp(transactor: Transactor[F]) =
    for {
      registry <- F.delay(SharedMetricRegistries.getOrCreate("default"))
      meteredRoutes = Metrics[F](Dropwizard(registry, "server"))(middleware(httpRoutes(transactor)))
      _ <- graphiteReporter(registry)
    } yield (meteredRoutes <+> metricsService(registry)).orNotFound

  def prometheusMeteredRoutes(
    transactor: Transactor[F]
  ) = prometheusReporter(middleware(httpRoutes(transactor)))

  val prometheusMeteredRoutes = prometheusReporter(middleware(httpRoutes))

  private def prometheusReporter(
    httpRoutes: HttpRoutes[F]
  ) =
    for {
      prometheusExportService <- PrometheusExportService.build[F]
      prometheusMetricsOps <- Prometheus.metricsOps(
        prometheusExportService.collectorRegistry,
        "server",
      )
    } yield Metrics(prometheusMetricsOps)(httpRoutes) <+> prometheusExportService.routes

  private def graphiteReporter(metricRegistry: MetricRegistry): F[Unit] =
    for {
      graphite <- F.delay(new Graphite(new InetSocketAddress("localhost", 2003)))
      reporter <- F.delay(
        GraphiteReporter
          .forRegistry(metricRegistry)
          .prefixedWith("web1.example.com")
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .filter(MetricFilter.ALL)
          .build(graphite)
      )
    } yield reporter.start(2, TimeUnit.SECONDS)

}

object HttpApi {
  def make[F[_]: Async]() = new HttpApi
}
