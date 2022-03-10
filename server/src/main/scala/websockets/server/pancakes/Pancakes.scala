package websockets.server.pancakes

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import org.typelevel.log4cats.Logger
import websockets.server.common.http.Controller

trait Pancakes[F[_]] {
  def controller: Controller[F]
}

object Pancakes {
  def make[F[_]: Async: Logger]: F[Pancakes[F]] =
    for {
      svc  <- PancakesService.make[F]
      cont <- PancakesController.make[F](svc)
    } yield new Pancakes[F] {
      override def controller: Controller[F] = cont
    }
}
