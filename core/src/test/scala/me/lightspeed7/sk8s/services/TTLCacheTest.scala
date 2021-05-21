package me.lightspeed7.sk8s.services

import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TTLCacheTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {
  import scala.language.postfixOps
  implicit val akka: ActorSystem = ActorSystem("TTLCACHE")

  override def afterAll(): Unit = Await.result(akka.terminate(), Duration.Inf)

  test("Cache testing") {

    val cache = TTLCache[String, Long](2 seconds, 500 milliseconds)

    cache.size shouldBe 0

    cache.put("foo", 123).size shouldBe 1
    cache.put("bar", 234).size shouldBe 2
    Thread.sleep(500)
    cache.getOrElseUpdate("baz", _ => 345) shouldBe Some(345)
    cache.size shouldBe 3

    // query
    cache.get("foo").getOrElse(0) shouldBe 123
    cache.get("bar").getOrElse(0) shouldBe 234
    cache.get("baz").getOrElse(0) shouldBe 345
    cache.size shouldBe 3

    Thread.sleep(1600)

    // query
    cache.size shouldBe 1
    cache.contains("baz") shouldBe true

    cache.get("foo").getOrElse(0) shouldBe 0
    cache.get("bar").getOrElse(0) shouldBe 0
    cache.getWithReset("baz").getOrElse(0) shouldBe 345

    Thread.sleep(1000)

    cache.get("baz").getOrElse(0) shouldBe 345

    Thread.sleep(2000)

    cache.get("baz").getOrElse(0) shouldBe 0

  }
}
