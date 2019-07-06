package io.timeli.sk8s

import org.joda.time.DateTime
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class AppInfoTest extends FunSuite {

  test("Generate AppInfo Object") {
    val info = AppInfo("name", "version", DateTime.now)

    Thread.sleep(5) // just make sure timestamps are different

    info.appName should be("name")
    info.version should be("version")
    Option(info.hostname) should not be None
    Option(info.ipAddress) should not be None
    info.buildTime.isBefore(DateTime.now)
  }
}