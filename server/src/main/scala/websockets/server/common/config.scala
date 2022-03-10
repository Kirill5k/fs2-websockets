package websockets.server.common

import cats.effect.Sync
import pureconfig.*
import pureconfig.generic.auto.*

object config {

  final case class ServerConfig(
      host: String,
      port: Int
  )

  final case class AppConfig(
      server: ServerConfig
  )

  object AppConfig {
    def load[F[_]](implicit F: Sync[F]): F[AppConfig] =
      F.blocking(ConfigSource.default.loadOrThrow[AppConfig])
  }
}
