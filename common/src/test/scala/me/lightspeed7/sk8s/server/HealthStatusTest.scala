package io.timeli.sk8s.server

import org.scalatest.{ BeforeAndAfterAll, FunSuiteLike, Matchers }

class HealthStatusTest extends FunSuiteLike with Matchers with BeforeAndAfterAll {

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
    HealthStatus.summary.toJson.toString shouldBe """{"sk8s":"health","overall_health":true,"tag1_health":true,"tag2_health":true}"""
  }

}