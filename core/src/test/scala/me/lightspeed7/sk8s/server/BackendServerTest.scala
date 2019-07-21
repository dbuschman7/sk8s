package me.lightspeed7.sk8s.server

import me.lightspeed7.sk8s.telemetry.TelemetryRegistry
import me.lightspeed7.sk8s.{ AppInfo, Constant, Sk8sContext, Sources, Variables }
import org.joda.time.DateTime
import org.scalatest.{ BeforeAndAfterAll, FunSuite, Matchers }
import play.api.libs.json.{ JsValue, Json }

class BackendServerTest extends FunSuite with BeforeAndAfterAll with Matchers {

  implicit val appInfo: AppInfo = AppInfo("application", "version", DateTime.now)

  implicit val ctx: Sk8sContext = Sk8sContext.create(appInfo)

  val export = new BackendServer(protobufFormet = false)
  val client = BackendServerClient()

  override def beforeAll(): Unit = {
    TelemetryRegistry.counter("metric")
    Variables.source[String](Sources.env, "A_STRING", Constant("A_VALUE"))
  }

  override def afterAll(): Unit =
    ctx.close()

  test("Test ping endpoint") {
    val response = client.ping
    println(response)

    response.headers.foreach { case (k, v) => println(f"$k%20s : $v%s") }

    response.code shouldBe 200
    response.body.right.get shouldBe "pong"
    response.headers
      .find { case (k, v) => k == "Content-Type" }
      .map(_._2)
      .getOrElse("oops") shouldBe "text/plain; charset=UTF-8"

  }

  test("Test health endpoint") {
    var response = client.health

    response.headers.foreach { case (k, v) => println(f"$k%20s : $v%s") }

    response.code shouldBe 200
    response.body.right.get shouldBe "OK"
    response.headers
      .find { case (k, v) => k == "Content-Type" }
      .map(_._2)
      .getOrElse("oops") shouldBe "text/plain; charset=UTF-8"

    HealthStatus.unhealthy("prometheus")

    response = client.health
    println(response)

    response.headers.foreach { case (k, v) => println(f"$k%20s : $v%s") }

    response.code shouldBe 418
    val body = response.body.left.get
    body.length should be > 0
    body.head shouldBe '{'
    body.last shouldBe '}'

    response.headers
      .find { case (k, v) => k == "Content-Type" }
      .map(_._2)
      .getOrElse("oops") shouldBe "text/plain; charset=UTF-8"

  }

  test("Test metrics endpoint") {
    val response = client.metrics
    response.code shouldBe 200
    val body    = response.body.right.get
    val bodyStr = new String(body)
    println(bodyStr)
    bodyStr.length should be > 1200

    val found = bodyStr.split("\n").filter(_.startsWith("sk8s_application_metric")).toList
    println(found)
    found.size shouldBe 1

  }

  test("Test TEXT config endpoint") {
    val response = client.configAsText
    response.code shouldBe 200
    val body    = response.body.right.get
    val bodyStr = new String(body)
    bodyStr.length should be > 300

    val found = bodyStr.split("\n").filter(_.contains("A_STRING")).toList
    println(found)
    found.size shouldBe 1

  }

  test("Test JSON config endpoint") {
    val response = client.configAsJson
    response.code shouldBe 200
    val body    = response.body.right.get
    val bodyStr = new String(body)
    println(bodyStr)
    bodyStr.length should be > 1200

    val bodyJson: JsValue = Json.parse(bodyStr)
    val pretty: String    = Json.prettyPrint(bodyJson)
    println(pretty)

    bodyStr.split("\n").filter(_.contains("runMode")).toList.size shouldBe 1
    bodyStr.split("\n").filter(_.contains("appInfo")).toList.size shouldBe 1
    bodyStr.split("\n").filter(_.contains("buildTime")).toList.size shouldBe 1
    bodyStr.split("\n").filter(_.contains("java")).toList.size shouldBe 1
    bodyStr.split("\n").filter(_.contains("memory")).toList.size shouldBe 1
    bodyStr.split("\n").filter(_.contains("processors")).toList.size shouldBe 1

    bodyStr.split("\n").filter(_.contains("dependencies")).toList.size shouldBe 1
    bodyStr.split("\n").filter(_.contains("artifact")).toList.size shouldBe 1

    bodyStr.split("\n").filter(_.contains("variables")).toList.size shouldBe 1
    bodyStr.split("\n").filter(_.contains("display")).toList.size shouldBe 1

  }
}
