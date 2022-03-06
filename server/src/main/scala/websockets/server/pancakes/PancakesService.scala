package websockets.server.pancakes

import fs2.Pipe
import websockets.server.pancakes.domain.{PancakeIngredient, PancakeStatus}

trait PancakesService[F[_]] {
  def bake(fryingPans: Int): F[Pipe[F, PancakeIngredient, PancakeStatus]]
}
