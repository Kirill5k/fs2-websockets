package websockets.server.health

import cats.effect.Async
import cats.effect.Ref
import cats.effect.Temporal
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import java.time.Instant
import org.http4s.HttpRoutes
import org.http4s.server.websocket.WebSocketBuilder2
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import websockets.server.common.http.Controller

final class HealthController[F[_]: Async](
    private val startupTime: Ref[F, Instant]
) extends Controller[F] {

  implicit val statusSchema: Schema[HealthController.AppStatus] = Schema.string

  private val statusEndpoint: ServerEndpoint[Any, F] = infallibleEndpoint.get
    .in("health" / "status")
    .out(jsonBody[HealthController.AppStatus])
    .serverLogicSuccess(_ => startupTime.get.map(t => HealthController.AppStatus(t)))

  override def routes(wsb: WebSocketBuilder2[F]): HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(statusEndpoint)
}

object HealthController {

  final case class AppStatus(startupTime: Instant)
  object AppStatus {
    implicit val appStatusCodec: Codec[AppStatus] = deriveCodec[AppStatus]
  }

  def make[F[_]: Async]: F[Controller[F]] =
    Temporal[F].realTimeInstant
      .flatMap(ts => Ref.of(ts))
      .map(ref => new HealthController[F](ref))
}
