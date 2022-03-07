package websockets.server.pancakes

import cats.effect.Async
import cats.effect.std.Queue
import fs2.Pipe
import websockets.server.pancakes.PancakesService.PortionIngredients
import websockets.server.pancakes.domain.PancakeIngredient.{Eggs, Flour, Milk}
import websockets.server.pancakes.domain.{PancakeIngredient, PancakeStatus}

trait PancakesService[F[_]] {
  def bake(fryingPans: Int): F[Pipe[F, PancakeIngredient, PancakeStatus]]
}

final private class LivePancakesService[F[_]] extends PancakesService[F] {

  override def bake(fryingPans: Int): F[Pipe[F, PancakeIngredient, PancakeStatus]] =
    Queue.bounded[F, PortionIngredients](24)
}

object PancakesService {

  final case class Ingredients(flourGrams: Int, milkLiters: Double, eggsCount: Int) {
    def +(i: PancakeIngredient): Ingredients = i match {
      case Flour(grams) => copy(flourGrams = flourGrams + grams)
      case Milk(liters) => copy(milkLiters = milkLiters + liters)
      case Eggs(count)  => copy(eggsCount = eggsCount + count)
    }
    def -(i: Ingredients): Ingredients = Ingredients(flourGrams - i.flourGrams, milkLiters - i.milkLiters, eggsCount - i.eggsCount)
    def *(n: Int): Ingredients = Ingredients(flourGrams * n, milkLiters * n, eggsCount * n)
  }

  object Ingredients {
    val Empty: Ingredients = Ingredients(0, 0.0, 0)
  }

  final case class PortionIngredients(ingredients: Ingredients, pancakeCount: Int)

  def make[F[_]: Async]: F[PancakesService[F]] =
    Async[F].pure(???)
}
