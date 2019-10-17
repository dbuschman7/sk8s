package me.lightspeed7.sk8s.util

import me.lightspeed7.sk8s.util.Time.TimerOutput
import org.scalatest.{ FunSuite, Matchers }

class TimeItTest extends FunSuite with Matchers {

  test("Single result") {
    var timerCalled = false

    val timer: TimerOutput = new TimerOutput {
      override def update(latencyInNanos: Long, count: Int): Unit = {
        timerCalled = true
        latencyInNanos.toInt should be >= 100
        count shouldBe 1
      }
    }

    val result = Time.it("single result") {
      Thread.sleep(100)
      1
    }(Some(timer))
    timerCalled shouldBe true
  }

  test("Sequence result") {
    var timerCalled = false

    implicit val timer: Some[TimerOutput] = Some(new TimerOutput {
      override def update(latencyInNanos: Long, count: Int): Unit = {
        timerCalled = true
        latencyInNanos.toInt should be >= 100
        count shouldBe 5
      }
    })
    val result = Time.it("single result") {
      Thread.sleep(100)
      (1 to 5)
    }
    timerCalled shouldBe true

  }
}
