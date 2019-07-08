package controllers

import me.lightspeed7.sk8s.{ BackgroundTasks, EnvironmentSource, PlayServerFunSuite, Sources }

class ApplicationTest extends PlayServerFunSuite {

  Sources.env.asInstanceOf[EnvironmentSource].overrideVariable(BackgroundTasks.ServerStartName, "true")

  override def beforeAllForSuite(): Unit = {}

  test("app can standup") {
    println("Success!")
  }
}
