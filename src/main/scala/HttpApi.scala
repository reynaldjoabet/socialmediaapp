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
import doobie.util.transactor
import org.http4s.server.Middleware
import org.typelevel.ci._

class HttpApi[F[_]](
  implicit F: Async[F]
) {

  // private  val middleware= AuthorizationMiddleware
  private val corsService = CORS
    .policy
    .withAllowOriginHost(Set("http://localhost:3000"))
    .withAllowMethodsIn(Set(POST, PUT, GET, DELETE))
    .withAllowCredentials(false) //set to true for csrf// The default behavior of cross-origin resource requests is for
    // requests to be passed without credentials like cookies and the Authorization header
    .withAllowHeadersIn(Set(ci"X-Csrf-Token", ci"Content-Type"))

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = { http: HttpRoutes[F] =>
    AutoSlash(http)
  } andThen { http: HttpRoutes[F] =>
    Timeout(60.seconds)(http)
  } andThen { http: HttpRoutes[F] =>
    corsService(http)
  }

  private val loggers = { http: HttpRoutes[F] =>
    RequestLogger.httpRoutes(true, true, _ => false)(http)
  } andThen { http: HttpRoutes[F] =>
    ResponseLogger.httpRoutes(true, true, _ => false)(http)
  }

  private val httpRoutes: HttpRoutes[F] =
    HealthRoutes[F].router <+> UserRoutes.make[F].router <+> LogoutRoutes[F].logoutRoutes <+>
      CommentRoutes.make[F].commentRoutes <+> PostRoutes.make[F].postRoutes <+> LikesRoutes
        .make[F]
        .likesRoutes <+> StoryRoutes.make[F].storyRoutes <+> RelationshipRoutes
        .make[F]
        .relationshipRoutes

  private def httpRoutes(transactor: Transactor[F]): HttpRoutes[F] =
    HealthRoutes[F].router <+> UserRoutes.make[F].router <+> LogoutRoutes[F].logoutRoutes <+>
      CommentRoutes.make[F](transactor).commentRoutes <+> PostRoutes
        .make[F](transactor)
        .postRoutes <+> LikesRoutes
        .make[F](transactor)
        .likesRoutes <+> StoryRoutes.make[F](transactor).storyRoutes <+> RelationshipRoutes
        .make[F](transactor)
        .relationshipRoutes <+> Auth0Routes.make[F](transactor).auth0Routes

  def middlewareHttpApp(transactor: Transactor[F]) =
    loggers.andThen(middleware)(httpRoutes(transactor)).orNotFound

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
  ) = prometheusReporter(loggers(middleware(httpRoutes(transactor))))

  val prometheusMeteredRoutes = prometheusReporter(loggers(middleware(httpRoutes)))

  private def prometheusReporter(
    httpRoutes: HttpRoutes[F]
  ) =
    for {
      prometheusExportService <- PrometheusExportService.build[F]
      prometheusMetricsOps <- Prometheus.metricsOps(
        prometheusExportService.collectorRegistry,
        "server"
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
  def make[F[_]: Async](): HttpApi[F] = new HttpApi

  private val cookieName = "csrf-token"

  private def token[F[_]: Async]() = CSRF.generateSigningKey[F]()

  def csrfService[F[_]: Async]()
    : F[Middleware[F, Request[F], Response[F], Request[F], Response[F]]] = token.map { key =>
    println(key.getAlgorithm())
    println(key.getEncoded().toList)
    val crsfBulider: CSRF.CSRFBuilder[F, F] = CSRF[F, F](
      key,
      request => CSRF.defaultOriginCheck[F](request, "localhost", Uri.Scheme.http, None)
    )
    crsfBulider // .withCookieName(cookieName)
      .withCookieDomain(Some("localhost"))
      .withCookiePath(Some("/"))
      .build
      .validate()
  }

  def csrfService1[F[_]: Async]() = CSRF
    .withGeneratedKey[F, F](request =>
      CSRF.defaultOriginCheck(request, "localhost", Uri.Scheme.http, None)
    )
    .map(builder =>
      builder
        .withCookieName(cookieName)
        .withCookieDomain(Some("localhost"))
        .build
        .validate()
    )

}
