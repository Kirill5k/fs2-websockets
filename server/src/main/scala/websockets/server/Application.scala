package websockets.server

import cats.effect.{IO, IOApp}
import websockets.server.common.config.AppConfig
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import websockets.server.health.Health

object Application extends IOApp.Simple {
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  override val run: IO[Unit] =
    for {
      config <- AppConfig.load[IO]
      health <- Health.make[IO]
      http   <- Http.make[IO](health)
      _      <- Server.serve[IO](config.server, http.app, runtime.compute).compile.drain
    } yield ()
}
