package me.lightspeed7.sk8s

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}

import play.api.libs.json.{JsResult, JsValue, Json}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class AppInfoTest extends AnyFunSuite with Matchers {

  val now = ZonedDateTime.now(ZoneOffset.UTC.normalized())
  test("Generate AppInfo Object") {
    val info = AppInfo("name", "version", now)

    Thread.sleep(5) // just make sure timestamps are different

    info.appName should be("name")
    info.version should be("version")
    Option(info.hostname) should not be None
    Option(info.ipAddress) should not be None
    info.buildTime.isBefore(now)

    val json: JsValue = Json.toJson(info)
    val str: String   = Json.prettyPrint(json)
    str should not be null

    val infoJs: JsValue              = Json.parse(str)
    val infoPrime: JsResult[AppInfo] = Json.fromJson[AppInfo](infoJs)
    infoPrime.isSuccess shouldBe true
    infoPrime.get shouldBe info
  }

  test("ZonedDateTime - serialization round trip") {

    val zNow: ZonedDateTime      = ZonedDateTime.now(ZoneOffset.UTC.normalized())
    val str                      = zNow.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    val zNowPrime: ZonedDateTime = ZonedDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    println("zNow    - " + zNow)
    println("zNow(s) - " + str)
    println("zNow'   - " + zNowPrime)

    zNow shouldBe zNowPrime
  }
}
