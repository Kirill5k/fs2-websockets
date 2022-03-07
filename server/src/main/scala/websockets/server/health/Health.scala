package websockets.server.health

import cats.effect.Async
import cats.syntax.functor._
import websockets.server.common.http.HttpController

trait Health[F[_]] {
  def httpController: HttpController[F]
}

object Health {
  def make[F[_]: Async]: F[Health[F]] =
    HealthController.make[F].map { healthController =>
      new Health[F] {
        override def httpController: HttpController[F] = healthController
      }
    }
}
