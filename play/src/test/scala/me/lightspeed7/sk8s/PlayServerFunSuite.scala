package me.lightspeed7.sk8s

import akka.actor.Props
import org.scalatest.Suite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

trait PlayServerFunSuite extends Sk8sFunSuite with GuiceOneServerPerSuite {
  self: Suite =>

  def beforeAllForSuite(): Unit

  //
  // Startup
  // //////////////////
  override final def beforeAll: Unit = {
    super.beforeAll()

    // must be last
    println("Calling beforeAllForSuite()")
    beforeAllForSuite()
  }

  override final def afterAll(): Unit =
    super.afterAll()

}
