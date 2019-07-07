package me.lightspeed7.sk8s.services

import me.lightspeed7.sk8s.AppInfo
import org.joda.time.DateTime
import org.scalatest.{ FunSuite, Matchers }
import play.api.libs.json.Json

class SystemPropertiesTest extends FunSuite with Matchers {

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
    libs.count(_.scalaVersion.length == 0) shouldBe 14
    libs.count(_.scalaVersion == "2.11") shouldBe 0
    libs.count(_.scalaVersion == "2.12") shouldBe 30
    //
    libs.foreach(println)

    val ah = libs.filter(_.artifact == "akka-http")
    ah.size shouldBe 1

    val ts = libs.filter(_.group == "com.typesafe.akka")
    ts.size shouldBe 6

    implicit val info: AppInfo = AppInfo("name", "version", DateTime.now)

    val appDeps = sysProps.appDependencies()
    val jsonStr = Json.prettyPrint(Json.toJson(appDeps))
    jsonStr.contains("appInfo") shouldBe true
    jsonStr.contains("name") shouldBe true

    jsonStr.contains("akka") shouldBe true

  }

}
