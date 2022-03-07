package websockets.server.common.http

import org.http4s.HttpRoutes
import org.http4s.server.websocket.WebSocketBuilder2
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.circe.TapirJsonCirce

trait Controller[F[_]] extends TapirJsonCirce with SchemaDerivation {
  def routes(wsb: WebSocketBuilder2[F]): HttpRoutes[F]
}
