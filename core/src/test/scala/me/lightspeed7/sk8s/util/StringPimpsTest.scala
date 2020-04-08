package me.lightspeed7.sk8s.util

import org.scalatest.{ FunSuite, Matchers }

class StringPimpsTest extends FunSuite with Matchers {

  import String._

  test("notNullTests") {
    "".notNull should be("")

    val foo: String = null
    foo.notNull should be("")
  }

  test("notEmptyTests") {
    "".notEmpty should be(None)
    "foo".notEmpty should be(Some("foo"))
  }

  test("notBlankTests") {
    "  foo ".notBlank should be(Some("foo"))
    "".notBlank should be(None)
  }

  test("padTests") {
    "foo".pad.left('-', 5) should be("--foo")
    "foo".pad.right('-', 5) should be("foo--")
  }

  test("strip quotes") {
    """"foo"""".stripQuotes shouldBe "foo"
    """"fo"oo"""".stripQuotes shouldBe """fo"oo"""

    """'foo'""".stripQuotes shouldBe "foo"
  }

  test("strip bracing") {
    """(foo)""".stripBracing shouldBe "foo"
    """{fo)oo}""".stripBracing shouldBe """fo)oo"""

    """[foo]""".stripBracing shouldBe "foo"

    """]foo{""".stripBracing shouldBe "]foo{"

  }
}
