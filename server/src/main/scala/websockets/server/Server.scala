package websockets.server

import cats.effect.Async
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import websockets.server.common.config.ServerConfig

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

object Server {
  def serve[F[_]: Async](config: ServerConfig, http: Http[F], ec: ExecutionContext): Stream[F, Unit] =
    BlazeServerBuilder[F]
      .withExecutionContext(ec)
      .bindHttp(config.port, config.host)
      .withResponseHeaderTimeout(3.minutes)
      .withIdleTimeout(1.hour)
      .withHttpWebSocketApp(http.app)
      .serve
      .drain
}
