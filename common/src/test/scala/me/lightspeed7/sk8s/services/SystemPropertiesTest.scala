package me.lightspeed7.sk8s.server.services

import org.scalatest.{FunSuite, Matchers}

class SystemPropertiesTest extends FunSuite with Matchers {

  test("Parse Raw test String") {
    sys.props.foreach { case (k, v) => println(f"$k%50s: $v") }

    val lines: Seq[String] = SysPropData.javaClassPath.split(":").toSeq
    lines.foreach(println)

    val sysProps = new SystemProperties(SysPropData.javaClassPath)

    val libs = sysProps.jarDependencies("symphony-local")
    libs.size shouldBe 127
    libs.count(_.scalaVersion.length == 0) shouldBe 56
    libs.count(_.scalaVersion == "2.11") shouldBe 0
    libs.count(_.scalaVersion == "2.12") shouldBe 71
    libs.count(_.group == "io.timeli") shouldBe 10

    libs.foreach(println)

    implicit val info = AppInfo("name", "version", DateTime.now)

    val appDeps = sysProps.appDependencies("symphony-local")
    val jsonStr = Json.prettyPrint(Json.toJson(appDeps))
    jsonStr.contains("appInfo") shouldBe true
    jsonStr.contains("name") shouldBe true

    jsonStr.contains("io.timeli") shouldBe true
    jsonStr.contains("akka") shouldBe true

  }

}
