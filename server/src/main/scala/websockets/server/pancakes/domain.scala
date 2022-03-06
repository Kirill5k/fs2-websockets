package websockets.server.pancakes

import io.circe.Codec
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto.deriveConfiguredCodec

object domain {

  sealed trait PancakeIngredient
  object PancakeIngredient {
    final case class Flour(grams: Int) extends PancakeIngredient
    final case class Milk(liters: Double) extends PancakeIngredient
    final case class Eggs(count: Int) extends PancakeIngredient

    private implicit val config: Configuration = Configuration.default
      .withDiscriminator("ingredient")
      .withSnakeCaseConstructorNames

    implicit val pancakeIngredientCodec: Codec[PancakeIngredient] = deriveConfiguredCodec[PancakeIngredient]
  }

  sealed trait PancakeStatus
  object PancakeStatus {
    case class IngredientReceived(ingredient: PancakeIngredient) extends PancakeStatus
    case object PancakeReady extends PancakeStatus

    private implicit val config: Configuration = Configuration.default
      .withDiscriminator("status")
      .withSnakeCaseConstructorNames

    implicit val pancakeStatusCodec: Codec[PancakeStatus] = deriveConfiguredCodec[PancakeStatus]
  }
}
