package websockets.server.health

import cats.effect.{IO, Ref}
import org.http4s.implicits._
import org.http4s._
import org.http4s.Header.Raw
import org.typelevel.ci.CIString
import websockets.server.ControllerSpec

import java.time.Instant

class HealthControllerSpec extends ControllerSpec {

  val ts = Instant.parse("2021-01-01T00:00:00Z")

  "A HealthController" should {

    "return status on the app" in {
      val controller = Ref.of[IO, Instant](ts).map(t => new HealthController[IO](t))

      val request  = Request[IO](uri = uri"/health/status", method = Method.GET, headers = Headers(Raw(CIString("foo"), "bar")))
      val response = controller.flatMap(_.routes(null).orNotFound.run(request))

      verifyJsonResponse(response, Status.Ok, Some(s"""{"startupTime":"$ts"}"""))
    }
  }
}
