package websockets.server.health

import cats.effect.Async
import cats.syntax.functor.*
import websockets.server.common.http.Controller

trait Health[F[_]] {
  def controller: Controller[F]
}

object Health {
  def make[F[_]: Async]: F[Health[F]] =
    HealthController.make[F].map { healthController =>
      new Health[F] {
        override def controller: Controller[F] = healthController
      }
    }
}
