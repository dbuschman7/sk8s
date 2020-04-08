package me.lightspeed7.sk8s.util

import me.lightspeed7.sk8s.Sk8sFunSuite
import org.scalatest.Matchers

class AlphaIdTest extends Sk8sFunSuite with Matchers {

  test("lowercase") {
    val id: String = AlphaId.randomLowerAlpha().id
    id.length shouldBe AlphaId.defaultLength

    val limit = id.toCharArray.toSet
    limit.min should be >= 'a'
    limit.max should be <= 'z'
  }

  test("uppercase") {
    val id = AlphaId.randomUpperAlpha().id
    id.length shouldBe AlphaId.defaultLength

    val limit = id.toCharArray.toSet
    limit.min should be >= 'A'
    limit.max should be <= 'Z'
  }

  test("all case") {
    val id: String = AlphaId.randomAlpha().id
    id.length shouldBe AlphaId.defaultLength

    val limit = id.toCharArray.toSet
    limit.min should be >= 'A'
    limit.max should be <= 'z'
  }

  test("length handling") {
    printIt(AlphaId.randomAlpha(3)).id.length shouldBe 3
    printIt(AlphaId.randomLowerAlpha(10)).id.length shouldBe 10
    printIt(AlphaId.randomUpperAlpha(20)).id.length shouldBe 20
    printIt(AlphaId.randomAlpha(300)).id.length shouldBe 300
  }

  test("Alpha upper with numerics") {
    val id = AlphaId.randomUpperAlphaWithNumerics(50)
    val numDigits = id.id.filter(_.isDigit).length
    println("NumDigits = " + numDigits)
    numDigits should be > 0
  }


  def printIt(in: AlphaId): AlphaId = {
    println(in)
    in
  }
}
