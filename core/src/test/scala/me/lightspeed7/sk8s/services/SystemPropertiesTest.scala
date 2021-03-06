package me.lightspeed7.sk8s.services

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

import me.lightspeed7.sk8s.AppInfo
import play.api.libs.json.Json
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SystemPropertiesTest extends AnyFunSuite with Matchers {

  ignore("get data") {
    val props = System.getProperties.getProperty("java.class.path")

    println(props)
  }

  test("Parse Raw test String") {
    sys.props.foreach { case (k, v) => println(f"$k%50s: $v") }

    val lines: Seq[String] = SysPropData.javaClassPath.split(":").toSeq
    lines.foreach(println)

    val sysProps = new SystemProperties(SysPropData.javaClassPath)

    val libs = sysProps.jarDependencies()
    libs.size shouldBe 44
    libs.count(_.scalaVersion.isEmpty) shouldBe 14
    libs.count(_.scalaVersion.getOrElse("") == "2.11") shouldBe 0
    libs.count(_.scalaVersion.getOrElse("") == "2.12") shouldBe 30
    //
    libs.foreach(println)

    val ah = libs.filter(_.artifact == "akka-http")
    ah.size shouldBe 1

    val ts = libs.filter(_.group == "com.typesafe.akka")
    ts.size shouldBe 6

    implicit val info: AppInfo = AppInfo("name", "version", ZonedDateTime.now(ZoneOffset.UTC.normalized()))

    val appDeps = sysProps.appDependencies()
    val jsonStr = Json.prettyPrint(Json.toJson(appDeps))
    jsonStr.contains("appInfo") shouldBe true
    jsonStr.contains("name") shouldBe true

    jsonStr.contains("akka") shouldBe true

  }

}
