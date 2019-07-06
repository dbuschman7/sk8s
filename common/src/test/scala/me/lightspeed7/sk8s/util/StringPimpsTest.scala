package me.lightspeed7.sk8s

import org.scalatest.FunSuite
import org.scalatest.Matchers.be

class StringPimpsTest extends FunSuite {

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
}