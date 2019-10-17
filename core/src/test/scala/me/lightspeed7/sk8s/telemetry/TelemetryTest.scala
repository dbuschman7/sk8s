package me.lightspeed7.sk8s.telemetry

import java.time.{ ZoneOffset, ZonedDateTime }
import java.util.concurrent.TimeUnit

import me.lightspeed7.sk8s.AppInfo
import me.lightspeed7.sk8s.util.PrettyPrint
import org.lyranthe.prometheus.client.registry.ProtoFormat
import org.scalatest.{ FunSuite, Matchers }
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

class TelemetryTest extends FunSuite with Matchers {

  implicit val appInfo: AppInfo = AppInfo("TelemetryTestApp", "0.0.0", ZonedDateTime.now(ZoneOffset.UTC.normalized()))

  test("initial setup and basics") {

    // setup
    val counter = TelemetryRegistry.counter("counter")
    val latency = TelemetryRegistry.latency("latency")
    val timer   = TelemetryRegistry.timer("timer")

    // load up the telemetry
    (1 to 1000).foreach { i =>
      timer.time {
        if (i % 100 == 0) print(".")
        counter.increment()
        latency.update(FiniteDuration(Random.nextInt(100), TimeUnit.MILLISECONDS))
      }
      Thread.sleep(Random.nextInt(10).toLong)
    }
    Thread.sleep(1 * 1000)

    // log exceptions
    TelemetryRegistry.initializeExceptionLogging

    val logger = LoggerFactory.getLogger("foo")
    logger.error("Logged IllegalStateException exception to follow - expected", new IllegalStateException("exception"))

    EventTelemetry.markEvent("eventName", "label1" -> "value1")

    // test results
    val metrics = TelemetryRegistry.snapshot.toSeq

    println()
    metrics.foreach(println)

    metrics.length should be > 5
    metrics.filterNot(_.name.name.startsWith("jvm_")).length should be > 0

    // test for exception logging channels
    import me.lightspeed7.sk8s.util.Snakify._
    println(this.getClass.getSimpleName.snakify)
    metrics.count(_.name.name.contains(this.getClass.getSimpleName.snakify)) should be > 0

    val binary: Array[Byte] = ProtoFormat.output(metrics.iterator)
    binary.length should be > 1600

    ProtoFormat.contentType should be("application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily; encoding=delimited")
  }

  //

  test("timer units ") {

    // undelying impl is seconds as a double
    val start = System.nanoTime()
    Thread.sleep(1)
    val end   = System.nanoTime
    val delta = (end - start) / 1e9

    println("Start   - " + start)
    println("End     - " + end)
    println("Seconds - " + delta)

    val timer = TelemetryRegistry.timer("timer2")
    timer.update(1000)
  }

  test("PrettyPrint latency") {
    PrettyPrint.latency(1L) shouldBe "0 microseconds"   // no support below micro
    PrettyPrint.latency(10L) shouldBe "0 microseconds"  // no support below micro
    PrettyPrint.latency(100L) shouldBe "0 microseconds" // no support below micro
    PrettyPrint.latency(1000L) shouldBe "1 microseconds"
    PrettyPrint.latency(10000L) shouldBe "10 microseconds"
    PrettyPrint.latency(100000L) shouldBe "100 microseconds"
    PrettyPrint.latency(1000000L) shouldBe "1 milliseconds"
    PrettyPrint.latency(10000000L) shouldBe "10 milliseconds"
    PrettyPrint.latency(100000000L) shouldBe "100 milliseconds"
    PrettyPrint.latency(1000000000L) shouldBe "1 seconds"
    PrettyPrint.latency(10000000000L) shouldBe "10 seconds"
    PrettyPrint.latency(100000000000L) shouldBe "1 mins 40 seconds"
    PrettyPrint.latency(1000000000000L) shouldBe "16 mins 40 seconds"
    PrettyPrint.latency(10000000000000L) shouldBe "166 mins 40 seconds" // no hours support
  }

}
