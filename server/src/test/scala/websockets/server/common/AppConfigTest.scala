package websockets.server.common

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import config.AppConfig
import websockets.server.CatsSpec

class AppConfigSpec extends CatsSpec {

  "An AppConfig" should {

    "load itself from reference.conf" in {
      AppConfig.load[IO].unsafeToFuture().map { config =>
        config.server.host mustBe "0.0.0.0"
      }
    }
  }
}
