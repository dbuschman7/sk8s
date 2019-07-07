package me.lightspeed7.sk8s

import org.scalatest.Matchers

class DockerImageTest extends Sk8sFunSuite with Matchers {

  test("Test tagRegex") {
    DockerImage.tagRegex.findAllMatchIn("3.7.123456.7890").toList.headOption.map(_.group(0)) shouldBe Some("3.7.123456.7890")
    DockerImage.tagRegex.findAllMatchIn("3.7.62df627").toList.headOption.map(_.group(0)) shouldBe Some("3.7.62df627")

    DockerImage.tagRegex.findAllMatchIn("18-1112-1352").toList.headOption.map(_.group(0)) shouldBe Some("18-1112-1352")
  }

  test("Test registry and repository parsing") {
    DockerImage.nameRegex.findAllMatchIn("docker.io/foo").toList.headOption.map(_.group(0)).get shouldBe "docker.io"
    DockerImage.nameRegex.findAllMatchIn("7docker.io/foo").toList.headOption.map(_.group(0)).get shouldBe "docker.io"
    DockerImage.nameRegex
      .findAllMatchIn("some-container")
      .toList
      .headOption
      .map(_.group(0))
      .get shouldBe "some-container"
  }

  test("Test image string parsing") {
    DockerImage.parse("docker.io/my-org/some-container:1.2.3").get /*        */ shouldBe DockerImage("docker.io",
                                                                                                     Some("my-org"),
                                                                                                     "some-container",
                                                                                                     Some("1.2.3"))
    DockerImage.parse("my-org/some-container:1.2.3").get /*                  */ shouldBe DockerImage("docker.io",
                                                                                                     Some("my-org"),
                                                                                                     "some-container",
                                                                                                     Some("1.2.3"))
    DockerImage.parse("my-org/some-container").get /*                                  */ shouldBe DockerImage("docker.io",
                                                                                                               Some("my-org"),
                                                                                                               "some-container",
                                                                                                               Some("latest"))
  }

  test("Test toString") {
    DockerImage("docker.io", Some("my-org"), "some-container", Some("1.2.3")).toString shouldBe "docker.io/my-org/some-container:1.2.3"
    DockerImage("docker.io", None, "some-container", Some("1.2.3")).toString shouldBe "docker.io/some-container:1.2.3"
    DockerImage("docker.io", None, "some-container", None).toString shouldBe "docker.io/some-container:latest"
  }

}
