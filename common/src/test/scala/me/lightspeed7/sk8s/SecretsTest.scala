package io.timeli.sk8s

import java.nio.file.{ Path, Paths }

import org.scalatest.{ FunSuite, Matchers }

class SecretsTest extends FunSuite with Matchers {

  val pwd: Path = Paths.get("sk8s").toAbsolutePath
  val resourcesPath: Path = Paths.get(pwd.toString + "/src/test/resources")
  val svcAcct: ServiceAccount = Sk8s.serviceAccount(resourcesPath)

  test("Read secret files") {
    val secretMount: VolumeFiles = Sk8s.secrets("secrets", encrypted = false, resourcesPath)
    secretMount.value("username").get shouldBe "username"
    secretMount.value("password").get shouldBe "password"
    secretMount.value("unknown") shouldBe None
  }

  test("Read configMaps files") {
    val configMap: VolumeFiles = Sk8s.configMap("config", resourcesPath)
    configMap.value("key1").get shouldBe "value1"
    configMap.value("key1").get shouldBe "value1"
  }

  test("Read encrypted secret files") {
    val secretMount: VolumeFiles = Sk8s.secrets("encrypted", encrypted = true, resourcesPath)
    secretMount.value("username").get shouldBe "username"
    secretMount.value("password").get shouldBe "password"
    secretMount.value("unknown") shouldBe None
  }
}
