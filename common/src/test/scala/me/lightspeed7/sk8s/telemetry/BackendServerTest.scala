package io.timeli.sk8s.telemetry

import akka.actor.ActorSystem
import io.timeli.sk8s.AppInfo
import io.timeli.sk8s.Sk8s.HealthStatus
import io.timeli.sk8s.util.Closeables
import org.joda.time.DateTime
import org.scalatest.{ BeforeAndAfterAll, FunSuite, Matchers }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.Try

class BackendServerTest extends FunSuite with BeforeAndAfterAll with Matchers {

  implicit val akka: ActorSystem = ActorSystem("Prometheus")
  implicit val ec: ExecutionContext = akka.dispatcher

  implicit val appInfo: AppInfo = AppInfo("application", "version", DateTime.now)

  val export = new BackendServer(protobufFormet = false, configGen = "CONFIG")
  val client = BackendServerClient()

  override def beforeAll(): Unit = {
    TelemetryRegistry.counter("metric")
  }

  override def afterAll(): Unit = {
    client.close()
    Try(Closeables.close()) // just ignore
    Await.result(akka.terminate(), Duration.Inf)
  }

  test("Test ping endpoint") {
    val response = client.ping
    println(response)

    response.headers.foreach { case (k, v) => println(f"$k%20s : $v%s") }

    response.code shouldBe 200
    response.body.right.get shouldBe "pong"
    response
      .headers
      .find { case (k, v) => k == "Content-Type" }
      .map(_._2)
      .getOrElse("oops") shouldBe "text/plain; charset=UTF-8"

  }

  test("Test health endpoint") {
    var response = client.health

    response.headers.foreach { case (k, v) => println(f"$k%20s : $v%s") }

    response.code shouldBe 200
    response.body.right.get shouldBe "OK"
    response
      .headers
      .find { case (k, v) => k == "Content-Type" }
      .map(_._2)
      .getOrElse("oops") shouldBe "text/plain; charset=UTF-8"

    HealthStatus.unhealthy("prometheus")

    response = client.health
    println(response)

    response.headers.foreach { case (k, v) => println(f"$k%20s : $v%s") }

    response.code shouldBe 418
    response.body.left.get shouldBe """{"sk8s":"health","overall_health":false,"prometheus_health":false}"""
    response
      .headers
      .find { case (k, v) => k == "Content-Type" }
      .map(_._2)
      .getOrElse("oops") shouldBe "text/plain; charset=UTF-8"

  }

  test("Test metrics endpoint") {
    val response = client.metrics
    response.code shouldBe 200
    val body = response.body.right.get
    val bodyStr = new String(body)
    println(bodyStr)
    bodyStr.length should be > 1200

    val found = bodyStr.split("\n").filter(_.startsWith("timeli_application_metric")).toList
    println(found)
    found.size shouldBe 1

  }
}
