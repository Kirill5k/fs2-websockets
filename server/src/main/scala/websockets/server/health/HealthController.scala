package websockets.server.health

import cats.effect.Async
import cats.effect.Ref
import cats.effect.Temporal
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import java.time.Instant
import org.http4s.HttpRoutes
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import websockets.server.common.http.HttpController

final class HealthController[F[_]: Async](
    private val startupTime: Ref[F, Instant]
) extends HttpController[F] {

  implicit val statusSchema: Schema[HealthController.AppStatus] = Schema.string

  private val statusEndpoint: ServerEndpoint[Any, F] = infallibleEndpoint.get
    .in("health" / "status")
    .out(jsonBody[HealthController.AppStatus])
    .serverLogicSuccess(_ => startupTime.get.map(t => HealthController.AppStatus(t)))

  def routes: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(statusEndpoint)
}

object HealthController {

  final case class AppStatus(startupTime: Instant)
  object AppStatus {
    implicit val appStatusCodec: Codec[AppStatus] = deriveCodec[AppStatus]
  }

  def make[F[_]: Async]: F[HttpController[F]] =
    Temporal[F].realTimeInstant
      .flatMap(ts => Ref.of(ts))
      .map(ref => new HealthController[F](ref))
}
