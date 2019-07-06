package io.timeli.sk8s.server

import io.timeli.sk8s.Sk8s
import org.scalactic.source
import org.scalatest.{ FunSuite, Matchers, Tag }

class MemoryCronActorTest extends FunSuite with Matchers {

  testIfNotK8s("Generate info") {
    val info = MemoryInfo.create()
    info.used should be > 0L
    info.percent should be < 100L
    info.free should be > 0L
    info.max should be > 0L
    info.total should be > 0L
    info.processors should be > 0

    val json = MemoryCronActor.generateJson(info)
    println(json)
    json.toString should include("""{"sk8s":"memory",""")
  }

  lazy val k8sActive: Boolean = Sk8s.isKubernetes()

  protected def testIfNotK8s(testName: String, testTags: Tag*)(testFun: => Any /* Assertion */ )(implicit pos: source.Position): Unit = {
    if (!k8sActive) test(testName, testTags: _*)(testFun)(pos)
    else ignore(testName + " !!! K8s REQUIRED ", testTags: _*)(testFun)(pos)
  }
}
