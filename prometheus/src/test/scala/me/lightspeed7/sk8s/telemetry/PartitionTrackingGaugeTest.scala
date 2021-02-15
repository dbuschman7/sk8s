package me.lightspeed7.sk8s.telemetry

import java.time.{ZoneOffset, ZonedDateTime}

import me.lightspeed7.sk8s.AppInfo
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class PartitionTrackingGaugeTest extends AnyFunSuite with Matchers {

  implicit val appInfo: AppInfo = AppInfo(this.getClass.getName, "0.0.0", ZonedDateTime.now(ZoneOffset.UTC.normalized()))
  test("test gauge tracks and resets properly") {

    val g = TelemetryRegistry.partitionTracker("test", "my-topic")
    g.name should be("test")
    g.topicName should be("my-topic")

    g.values.isEmpty should be(true)
    g.asString should be("""{"my-topic": []}""")

    g.set(1, 1001)
    g.values should be(Seq(1 -> 1001d))
    g.asString should be("""{"my-topic": [{1: 1001}]}""")

    g.set(20, 10)
    g.values should be(Seq(1 -> 1001d, 20 -> 10d))
    g.asString should be("""{"my-topic": [{1: 1001}, {20: 10}]}""")

    g.set(3, 900)
    g.values should be(Seq(1 -> 1001d, 3 -> 900d, 20 -> 10d))
    g.asString should be("""{"my-topic": [{1: 1001}, {3: 900}, {20: 10}]}""")

    g.set(1, 900)
    g.values should be(Seq(1 -> 1001d, 3 -> 900d, 20 -> 10d))
    g.asString should be("""{"my-topic": [{1: 1001}, {3: 900}, {20: 10}]}""")

    g.set(1, 9000)
    g.values should be(Seq(1 -> 9000d, 3 -> 900d, 20 -> 10d))
    g.asString should be("""{"my-topic": [{1: 9000}, {3: 900}, {20: 10}]}""")

    g.reset()
    g.values.isEmpty should be(true)
    g.asString should be("""{"my-topic": []}""")

    g.set(1, 9001)
    g.values should be(Seq(1 -> 9001d))
    g.asString should be("""{"my-topic": [{1: 9001}]}""")
  }
}
