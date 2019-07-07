package me.lightspeed7.sk8s

import java.nio.file.Paths

import org.scalatest.{ FunSuite, Matchers }

class ServiceAccountTest extends FunSuite with Matchers {

  test("Test default - no files existing in testing") {
    val svcAcct = Sk8s.serviceAccount()
    svcAcct.basePath.toString should be("/var/run/secrets/kubernetes.io/serviceaccount")
  }

  test("Test resources setup -- with files") {
    val pwd           = Paths.get("common").toAbsolutePath
    val resourcesPath = Paths.get(pwd.toString + "/src/test/resources")
    val svcAcct       = Sk8s.serviceAccount(resourcesPath)

    { // test correct path
      val fullPath = svcAcct.basePath.toString
      val tail     = fullPath.substring(fullPath.indexOf("common"))
      tail should be("common/src/test/resources/kubernetes.io/serviceaccount")
    }

    { // test token lookup
      val token = svcAcct.token
      token should not be null
      token.length should be(846)
    }

    { // test ca.crt lookup
      val crt = svcAcct.crt
      crt should not be null
      crt.length should be(1066)
    }

    { // test namespace lookup
      val namespace = svcAcct.namespace
      namespace should not be null
      namespace.length should be(7)
    }
  }

}