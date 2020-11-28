package me.lightspeed7.sk8s.files

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class VolumeFilesTest extends AnyFunSuite with Matchers {

  import TestCrypto._
  import me.lightspeed7.sk8s.util.String._

  test("test base64 round trip") {

    val encoded: String = crypto.toBase64(data)
    encoded.notBlank.isDefined shouldBe true

    val thisData: Option[String] = crypto.fromBase64(encoded)
    thisData.isDefined shouldBe true
    thisData.get shouldBe data
  }

  test("test encrypted round trip") {

    val encrypted: Option[String] = crypto.encrypt(data)
    encrypted.isDefined shouldBe true

    val thisData: Option[String] = crypto.decrypt(encrypted.get)
    thisData.isDefined shouldBe true
    thisData.get shouldBe data
  }

}
