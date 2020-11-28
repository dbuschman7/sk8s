package me.lightspeed7.sk8s.manifests

import me.lightspeed7.sk8s.RunMode
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class Sk8sConfigMapTest extends AnyFunSuite with Matchers {

  test("generate example config map") {

    val generated: String =
      Sk8sConfigMap.generateSk8sConfig("cluster", "namespace", RunMode.Developer, Map("foo" -> "bar", "bar" -> "baz"))

    println("*****************************")
    println(generated)
    println("*****************************")
    generated.contains("kind: ConfigMap") shouldBe true
    generated.contains("name: sk8s-config") shouldBe true
    generated.contains("namespace: namespace") shouldBe true

    generated.contains("cluster-name: cluster") shouldBe true
    generated.contains("sk8s-run-mode: Developer") shouldBe true

    generated.contains("foo : bar") shouldBe true

  }
}
