package io.timeli.sk8s.util

import org.scalatest.{ FunSuite, Matchers }

class AutoCloseTest extends FunSuite with Matchers {

  test("Test Single for comprehension") {
    var codeCalled: Boolean = false

    val obj: DemoCloseable = for (v <- AutoClose(new DemoCloseable("abc"))) {
      codeCalled = true
      v
    }
    codeCalled should be(true)
    obj.closed should be(true)
  }

  test("Test multi-line for comprehension") {
    var codeCalled: Boolean = false

    val (aR, bR, cR) = for (
      a <- AutoClose(new DemoCloseable("a123"));
      b <- AutoClose(new DemoCloseable("b123"));
      c <- AutoClose(new DemoCloseable("c123"))
    ) yield {
      codeCalled = true
      (a, b, c)
    }

    codeCalled should be(true)
    aR.closed should be(true)
    bR.closed should be(true)
    cR.closed should be(true)
  }
}

class DemoCloseable(val s: String) extends AutoCloseable {
  var closed = false

  override def close(): Unit = {
    closed = true
  }
}

object DemoCloseable {
  def unapply(dc: DemoCloseable): Option[(String)] = Some(dc.s)
}