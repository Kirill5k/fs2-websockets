package websockets.server

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe.parser._
import org.http4s.{Response, Status}
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

trait ControllerSpec extends AnyWordSpec with Matchers {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def verifyJsonResponse(
      response: IO[Response[IO]],
      expectedStatus: Status,
      expectedBody: Option[String] = None
  ): Assertion =
    response
      .flatMap { res =>
        expectedBody match {
          case Some(expectedJson) =>
            res.as[String].map { receivedJson =>
              res.status mustBe expectedStatus
              parse(receivedJson) mustBe parse(expectedJson)
            }
          case None =>
            res.body.compile.toVector.map { receivedJson =>
              res.status mustBe expectedStatus
              receivedJson mustBe empty
            }
        }
      }
      .unsafeRunSync()
}
