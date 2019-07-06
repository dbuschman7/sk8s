package me.lightspeed7.sk8s

import org.scalatest.{FunSuite, Matchers}

class VolumeFilesTest extends FunSuite with Matchers {

  val data: String = "And I'm thinking you weren't burdened with an overabundance of schooling."

  test("test base64 round trip") {
    val encoded: String = VolumeFiles.toBase64(data)
    encoded.notBlank.isDefined shouldBe true

    val thisData: Option[String] = VolumeFiles.fromBase64(encoded)
    thisData.isDefined shouldBe true
    thisData.get shouldBe data
  }

  test("test encrypted round trip") {

    val encrypted: Option[String] = encrypt(data)
    encrypted.isDefined shouldBe true

    val thisData: Option[String] = decrypt(encrypted.get)
    thisData.isDefined shouldBe true
    thisData.get shouldBe data
  }

}
