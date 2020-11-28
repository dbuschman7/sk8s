package me.lightspeed7.sk8s.server

import org.scalatest.BeforeAndAfterAll
import play.api.libs.json.{ JsBoolean, JsString }
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class HealthStatusTest extends AnyFunSuiteLike with Matchers with BeforeAndAfterAll {

  test("Health Check") {
    HealthStatus.healthy("tag1")
    HealthStatus.isHealthy shouldBe true

    HealthStatus.healthy("tag2")
    HealthStatus.isHealthy shouldBe true

    HealthStatus.unhealthy("tag1")
    HealthStatus.isHealthy shouldBe false

    HealthStatus.healthy("tag1")
    HealthStatus.isHealthy shouldBe true

    // json formatting
    println(HealthStatus.summary.toJson.toString)

    HealthStatus.summary.toJson.fields.toSet shouldBe Set(
      "sk8s"           -> JsString("health"),
      "overall_health" -> JsBoolean(true),
      "tag1_health"    -> JsBoolean(true),
      "tag2_health"    -> JsBoolean(true)) //  """{"sk8s":"health","overall_health":true,"tag1_health":true,"tag2_health":true}"""
  }

}
