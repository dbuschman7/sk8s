package me.lightspeed7.sk8s

import com.softwaremill.sttp.{ HttpURLConnectionBackend, Id, SttpBackend }
import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.server.BackendServerClient
import me.lightspeed7.sk8s.util.AutoClose
import org.joda.time.DateTime
import org.scalatest.Matchers
import play.api.libs.json.{ JsObject, Json }

import scala.concurrent.Future

class ApplicationTest extends Sk8sFunSuite with Matchers with LazyLogging {

  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  lazy val client: BackendServerClient = BackendServerClient()(ctx)

  test("returns secondary endpoints") {

    try {
      for (app <- AutoClose(new BackendApplication(AppInfo(BuildInfo.name, BuildInfo.version, new DateTime(BuildInfo.buildTime))))) {
        import app._

        Thread.sleep(8000)
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

        val configResponse = client.configAsJson
        configResponse.code shouldBe 200
        val body: String = configResponse.body.right.get
        println("Body -> " + body)
        val jsValue = Json.parse(body)

        println("JsValue -> " + jsValue.getClass.toString)
        jsValue.isInstanceOf[JsObject] shouldBe true
      }
      println("tests completed")
    } finally {
      // shutdown
      BackendApplication.shutdown
      Thread.sleep(4000)
    }
    println("tests exiting")
  }

}
