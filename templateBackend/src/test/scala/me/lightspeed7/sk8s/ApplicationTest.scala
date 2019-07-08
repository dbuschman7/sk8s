package me.lightspeed7.sk8s

import akka.actor.ActorSystem
import com.softwaremill.sttp.{ HttpURLConnectionBackend, Id, SttpBackend }
import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.telemetry.BackendServerClient
import org.scalatest.Matchers

import scala.concurrent.Future

class ApplicationTest extends Sk8sFunSuite with Matchers with LazyLogging {

  Sources.env.asInstanceOf[EnvironmentSource].overrideVariable(BackgroundTasks.ServerStartName, "true")

  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  lazy val client: BackendServerClient = BackendServerClient()(ActorSystem("prometheus"))

  test("returns secondary endpoints") {

    try {
      val app = Future {
        Application.main(Array())

      }
      Thread.sleep(4000)
      println("tests starting ... ")

      var response = client.ping
      response.code shouldBe 200
      response.body.right.get shouldBe "pong"

      response = client.health
      response.code shouldBe 200
      response.body.right.get shouldBe "OK"

      val rawResponse = client.metrics
      rawResponse.code shouldBe 200
      rawResponse.body.right.get.length should be > 0

      println("tests completed")

    } finally {
      // shutdown
      BackendApplication.shutdown
      Thread.sleep(4000)
    }
    println("tests exiting")

  }

}
