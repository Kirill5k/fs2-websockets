package websockets.server.pancakes

import cats.effect.Async
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.functor._
import fs2.Pipe
import org.http4s.HttpRoutes
import org.http4s.server.websocket.WebSocketBuilder2
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint.Full
import sttp.tapir.server.http4s._
import websockets.server.common.http.Controller
import websockets.server.pancakes.domain.{PancakeIngredient, PancakeStatus}

final private class PancakesController[F[_]: Async](
    private val pancakesService: PancakesService[F]
) extends Controller[F] {

  private val pansQueryInput: EndpointInput.Query[Int] = query[Int]("pans")
    .description("The number of frying pans to use in parallel")
    .example(2)
    .validate(Validator.min(1))

  private val pancakesEndpoint: Endpoint[Unit, Int, String, Pipe[F, PancakeIngredient, PancakeStatus], Fs2Streams[F] with WebSockets] =
    endpoint
      .in("pancakes")
      .in(pansQueryInput)
      .errorOut(stringBody)
      .out(webSocketBody[PancakeIngredient, CodecFormat.Json, PancakeStatus, CodecFormat.Json](Fs2Streams[F]))

  private val pancakesRoute: Full[Unit, Unit, Int, String, Pipe[F, PancakeIngredient, PancakeStatus], Fs2Streams[F] with WebSockets, F] =
    pancakesEndpoint
      .serverLogic { pans =>
        pancakesService
          .bake(pans)
          .map(_.asRight[String])
          .handleError(_.getMessage.asLeft[Pipe[F, PancakeIngredient, PancakeStatus]])
      }

  override def routes(wsb: WebSocketBuilder2[F]): HttpRoutes[F] =
    Http4sServerInterpreter[F]().toWebSocketRoutes(pancakesRoute)(wsb)
}

object PancakesController {
  def make[F[_]: Async](service: PancakesService[F]): F[Controller[F]] =
    Async[F].pure(new PancakesController[F](service))
}
