package websockets.server

import cats.Monad
import cats.effect.Async
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware._
import websockets.server.health.Health

import scala.concurrent.duration._

final class Http[F[_]: Async] private (
    private val health: Health[F]
) {

  private val routes: HttpRoutes[F] = health.httpController.routes

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = { (http: HttpRoutes[F]) => AutoSlash(http) }
    .andThen((http: HttpRoutes[F]) => CORS.policy.withAllowOriginAll.withAllowCredentials(false).apply(http))
    .andThen((http: HttpRoutes[F]) => Timeout(60.seconds)(http))

  private val loggers: HttpApp[F] => HttpApp[F] = { (http: HttpApp[F]) => RequestLogger.httpApp(true, true)(http) }
    .andThen((http: HttpApp[F]) => ResponseLogger.httpApp(true, true)(http))

  val app: HttpApp[F] = loggers(middleware(routes).orNotFound)
}

object Http {
  def make[F[_]: Async](health: Health[F]): F[Http[F]] = Monad[F].pure(new Http[F](health))
}