package websockets.server.pancakes

import cats.effect.Async
import cats.effect.std.Queue
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.{Pipe, Stream}
import org.typelevel.log4cats.Logger
import websockets.server.pancakes.PancakesService.{Ingredients, PortionIngredients}
import websockets.server.pancakes.domain.PancakeIngredient.{Eggs, Flour, Milk}
import websockets.server.pancakes.domain.PancakeStatus.{IngredientReceived, PancakeReady}
import websockets.server.pancakes.domain.{PancakeIngredient, PancakeStatus}

import scala.concurrent.duration._

trait PancakesService[F[_]] {
  def bake(fryingPans: Int): F[Pipe[F, PancakeIngredient, PancakeStatus]]
}

final private class LivePancakesService[F[_]](implicit
    F: Async[F],
    logger: Logger[F]
) extends PancakesService[F] {

  override def bake(fryingPans: Int): F[Pipe[F, PancakeIngredient, PancakeStatus]] =
    Queue
      .bounded[F, PortionIngredients](24)
      .map(q => in => accumulate(in, q).merge(fry(fryingPans, q)))

  private def accumulate(
      input: Stream[F, PancakeIngredient],
      fryingQueue: Queue[F, PortionIngredients]
  ): Stream[F, IngredientReceived] =
    input
      .evalTap(ingredient => logger.info(s"Received ingredient: $ingredient"))
      .evalMapAccumulate(Ingredients.Empty) { case (ingredients, ingredient) =>
        val newIngredients = ingredients + ingredient

        val (remainingIngredients, portionIngredients) = PortionIngredients.from(newIngredients)

        val enqueueFrying = portionIngredients match {
          case None => F.unit
          case Some(pi) =>
            logger.info(s"Frying pancakes: $pi; remaining ingredients: $remainingIngredients") >> fryingQueue.offer(pi)
        }

        enqueueFrying.map(_ => (remainingIngredients, IngredientReceived(ingredient)))
      }
      .map(_._2)

  // description of a process which dequeues ingredients from the given `fryingQueue`, and fries them using
  // up to `fryingPans` in parallel
  private def fry(fryingPans: Int, fryingQueue: Queue[F, PortionIngredients]): Stream[F, PancakeReady.type] =
    Stream
      .fromQueueUnterminated(fryingQueue)
      .flatMap { portionIngredients =>
        val singlePanPancakesCount = portionIngredients.pancakeCount / fryingPans
        val extraPancakesCount     = portionIngredients.pancakeCount % fryingPans

        // assigning the number of pancakes to fry to each pan; distributing evenly as possible, and adding one extra
        // pancake to the remaining ones
        val pancakesPerFryingPan = (1 to fryingPans).map(i => singlePanPancakesCount + (if (i <= extraPancakesCount) 1 else 0))

        Stream(pancakesPerFryingPan: _*) // a stream of numbers
          .map(fryingPan)                // a stream of stream descriptions
          .parJoinUnbounded              // flattening the stream of streams by running them all in parallel
      }

  // a process simulating the behavior of a single frying pan: we assume that the ingredients are ready; frying
  // one pancake takes one second
  private def fryingPan(count: Int): Stream[F, PancakeReady.type] =
    Stream
      .awakeEvery[F](1.second)
      .map(_ => PancakeReady)
      .take(count.toLong)
}

object PancakesService {

  final case class Ingredients(flourGrams: Int, milkLiters: Double, eggsCount: Int) {
    def +(i: PancakeIngredient): Ingredients = i match {
      case Flour(grams) => copy(flourGrams = flourGrams + grams)
      case Milk(liters) => copy(milkLiters = milkLiters + liters)
      case Eggs(count)  => copy(eggsCount = eggsCount + count)
    }
    def -(i: Ingredients): Ingredients = Ingredients(flourGrams - i.flourGrams, milkLiters - i.milkLiters, eggsCount - i.eggsCount)
    def *(n: Int): Ingredients         = Ingredients(flourGrams * n, milkLiters * n, eggsCount * n)
  }

  object Ingredients {
    val Empty: Ingredients = Ingredients(0, 0.0, 0)
  }

  final case class PortionIngredients(ingredients: Ingredients, pancakeCount: Int)

  object PortionIngredients {
    val SinglePortion: Ingredients = Ingredients(250, 0.35, 2) // e.g. https://www.kwestiasmaku.com/kuchnia_polska/nalesniki/nalesniki.html
    val SinglePortionPancakeCount  = 10

    /** @return
      *   If enough ingredients are accumulated, the remaining ingredients and the ingredients necessary for frying, along with the number
      *   of pancakes that can be fried with them
      */
    def from(ingredients: Ingredients): (Ingredients, Option[PortionIngredients]) = {
      val portions = Math.min(
        ingredients.flourGrams / SinglePortion.flourGrams,
        Math.min((ingredients.milkLiters / SinglePortion.milkLiters).toInt, ingredients.eggsCount / SinglePortion.eggsCount)
      )
      if (portions == 0) {
        (ingredients, None)
      } else {
        // ingredient math!
        val portionIngredients = SinglePortion * portions
        (ingredients - portionIngredients, Some(PortionIngredients(portionIngredients, SinglePortionPancakeCount * portions)))
      }
    }
  }

  def make[F[_]: Async: Logger]: F[PancakesService[F]] =
    Async[F].pure(new LivePancakesService[F]())
}
