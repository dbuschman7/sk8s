package me.lightspeed7.sk8s

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class EnvironmentSourceTest extends AnyFunSuite with Matchers {

  EnvironmentSource.overrideVariable("ENV_INT", "12345")

  EnvironmentSource.overrideVariable("MY_POD_IP", "172.17.0.10")
  EnvironmentSource.overrideVariable("MY_POD_NAME", "hello-2355654925-hjf7b")
  EnvironmentSource.overrideVariable("MY_POD_NAMESPACE", "default")

  EnvironmentSource.overrideVariable("MY_CPU_LIMIT", "1")
  EnvironmentSource.overrideVariable("MY_CPU_REQUEST", "1")
  EnvironmentSource.overrideVariable("MY_MEM_LIMIT", "67108864")
  EnvironmentSource.overrideVariable("MY_MEM_REQUEST", "33554432")

  EnvironmentSource.overrideVariable("KUBERNETES_PORT", "tcp://10.0.0.1:443")
  EnvironmentSource.overrideVariable("KUBERNETES_PORT_443_TCP", "tcp://10.0.0.1:443")
  EnvironmentSource.overrideVariable("KUBERNETES_PORT_443_TCP_ADDR", "10.0.0.1")
  EnvironmentSource.overrideVariable("KUBERNETES_PORT_443_TCP_PORT", "443")
  EnvironmentSource.overrideVariable("KUBERNETES_PORT_443_TCP_PROTO", "tcp")
  EnvironmentSource.overrideVariable("KUBERNETES_SERVICE_HOST", "10.0.0.1")
  EnvironmentSource.overrideVariable("KUBERNETES_SERVICE_PORT", "443")
  EnvironmentSource.overrideVariable("KUBERNETES_SERVICE_PORT_HTTPS", "443")

  test("Environment") {
    EnvironmentSource.valueInt("ENV_INT", 0) should be(12345)
    EnvironmentSource.value("ENV_INT", "0") should be("12345")

    Sk8s.MyPod.namespace should be("default")
    Sk8s.MyPod.name should be("hello-2355654925-hjf7b")
    Sk8s.MyPod.ip should be("172.17.0.10")

    Sk8s.MyCpu.limit should be(1)
    Sk8s.MyCpu.request should be(1)
    Sk8s.MyMemory.limit should be(67108864)
    Sk8s.MyMemory.request should be(33554432)

    val kube = Sk8s.service("kubernetes")
    kube.port should be(443)
    kube.host should be("10.0.0.1")
    kube.secure should be(true)
    kube.getUrl should be("https://10.0.0.1:443")

  }

}
