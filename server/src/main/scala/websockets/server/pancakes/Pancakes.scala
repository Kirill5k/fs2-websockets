package websockets.server.pancakes

import cats.effect.Async
import org.http4s.server.websocket.WebSocketBuilder2

trait Pancakes[F[_]] {
  def webSocketController: WebSocketBuilder2[F]
}

object Pancakes {
  def make[F[_]: Async]: F[Pancakes[F]] =
    for {
      service    <- PancakesService.make[F]
      controller <- PancakesController.make[F](service)
    } yield new Pancakes[F] {
      override def webSocketController: WebSocketBuilder2[F] = controller
    }
}
