package me.lightspeed7.sk8s

import java.time.ZonedDateTime

import com.softwaremill.sttp.{HttpURLConnectionBackend, Id, SttpBackend}
import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.backend.{BackendApplication, BackendServerClient}
import me.lightspeed7.sk8s.util.{AlphaId, AutoClose}
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsObject, Json}

class ApplicationTest extends Sk8sFunSuite with Matchers with LazyLogging {

  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  lazy val client: BackendServerClient = BackendServerClient()(ctx)

  test("get alphaId") {
    (1 to 10).map(_ -> AlphaId.randomLowerAlphaWithNumerics(20)).foreach(println)
  }

  test("returns secondary endpoints") {

    try {
      for (app <- AutoClose(new BackendApplication(AppInfo(BuildInfo.name, BuildInfo.version, ZonedDateTime.now())))) {
        import app._
        println("tests starting ... ")

        var response = client.ping
        response.code shouldBe 200
        response.body.right.get shouldBe "pong"

        response = client.health
        // test fails is backend server test is running at the same time

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
