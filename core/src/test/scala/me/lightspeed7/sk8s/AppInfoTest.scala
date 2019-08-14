package me.lightspeed7.sk8s

import java.time.format.DateTimeFormatter
import java.time.{ ZoneOffset, ZonedDateTime }

import org.scalatest.{ FunSuite, Matchers }
import org.scalatest.Matchers._

class AppInfoTest extends FunSuite with Matchers {

  val now = ZonedDateTime.now(ZoneOffset.UTC.normalized())
  test("Generate AppInfo Object") {
    val info = AppInfo("name", "version", now)

    Thread.sleep(5) // just make sure timestamps are different

    info.appName should be("name")
    info.version should be("version")
    Option(info.hostname) should not be None
    Option(info.ipAddress) should not be None
    info.buildTime.isBefore(now)
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
