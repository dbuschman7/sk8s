package me.lightspeed7.sk8s

import org.scalatest.Matchers

class Sk8sContextTest extends Sk8sFunSuite with Matchers {

  test("can the Sk8sContext standup") {

    ctx.appInfo.appName shouldBe this.getClass.getName
    ctx.ec.getClass.getName shouldBe "akka.dispatch.Dispatcher"

  }
}
