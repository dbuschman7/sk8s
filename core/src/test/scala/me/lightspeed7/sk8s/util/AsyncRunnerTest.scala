package me.lightspeed7.sk8s.util

import java.util.concurrent.atomic.AtomicInteger

import me.lightspeed7.sk8s.Sk8sFunSuite
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.util.Try

class AsyncRunnerTest extends Sk8sFunSuite with BeforeAndAfterAll with Matchers {

  override def afterAll(): Unit = {
    Try(ctx.close()) // just ignore
    super.afterAll()
  }

  test("Run multiple times test") {

    val atomic = new AtomicInteger(0)

    AsyncRunner.runFor(5 seconds, 500 milliseconds) {
      atomic.incrementAndGet()
      ()
    }

    Thread.sleep((6 seconds).toMillis)

    println(s"Iterations = ${atomic.get()}")

    atomic.get() should be > 0
    atomic.get() should be < 11
  }

}
