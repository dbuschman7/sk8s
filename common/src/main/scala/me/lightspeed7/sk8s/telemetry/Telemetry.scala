package me.lightspeed7.sk8s.telemetry

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.lyranthe.prometheus.client.registry.RegistryMetrics
import org.lyranthe.prometheus.client.{ Counter, Gauge, _ }
import me.lightspeed7.sk8s.AppInfo
import me.lightspeed7.sk8s.util.Time
import org.joda.time.DateTime
import org.slf4j.{ Logger, LoggerFactory }

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

//
// Telemetry Types
// ////////////////////////////////////
sealed trait Telemetry {

  implicit class Snakify(in: String) {
    def snakify: String =
      in.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2").replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase

    def snakeClassname: String = in.replaceAll("\\.", "_").toLowerCase
  }

  def getType: String // define this in sub classes

  def toMetricName(name: String, appInfo: AppInfo) =
    MetricName("sk8s_" + appInfo.appName.snakify + "_" + name.snakify.replace("-", "_"))
}

sealed trait TimerLike {
  def time[A](f: => A): A

  def time[A](f: => Future[A])(implicit ec: ExecutionContext): Future[A]

  def update(latencyInSeconds: Double): Unit

  def update(latencyInMillis: Long): Unit

  def update(latency: Duration): Unit

  def deltaFrom(startTime: DateTime): Long

  def deltaFrom(startTimeMillis: Long): Long
}

final case class BasicGauge(name: String, appInfo: AppInfo)(implicit reg: Registry) extends Telemetry {
  val gauge: LabelledGauge = Gauge(toMetricName(name, appInfo), name)
    .labels(label"version", label"type")
    .register
    .labelValues(appInfo.version, getType)

  def incBy(v: Double): Unit = gauge.incBy(v)

  def inc(): Unit = gauge.inc()

  def decBy(v: Double): Unit = gauge.decBy(-v)

  def dec(): Unit = gauge.dec()

  def set(v: Double): Unit = gauge.set(v)

  def update(v: Long): Unit = gauge.set(v.toDouble)

  override def getType: String = "gauge"
}

final case class BasicCounter(name: String, appInfo: AppInfo)(implicit reg: Registry) extends Telemetry {
  val counter: LabelledCounter = Counter(toMetricName(name, appInfo), name)
    .labels(label"version", label"type")
    .register
    .labelValues(appInfo.version, getType)

  def increment(): Unit = counter.inc()

  def incrementBy(count: Long): Unit = counter.incBy(count)

  override def getType: String = "counter"

}

final case class BasicTimer(name: String, appInfo: AppInfo)(implicit reg: Registry, buckets: HistogramBuckets) extends Telemetry with TimerLike {
  val histo: LabelledHistogram = Histogram(toMetricName(name, appInfo), name)
    .labels(label"version", label"type")
    .register
    .labelValues(appInfo.version, getType)

  def time[A](f: => A): A = {
    val (nanos, result) = Time.thisBlock(f)
    val secs: Double    = nanos.toDouble / (1000.0 * 1000.0)
    update(secs)
    result
  }

  def time[A](f: => Future[A])(implicit ec: ExecutionContext): Future[A] =
    Time.thisBlock(f).map {
      case (nanos, result) =>
        val secs: Double = nanos.toDouble / (1000.0 * 1000.0)
        update(secs)
        result
    }

  def update(latencyInSeconds: Double): Unit =
    histo.observe(latencyInSeconds) // units are doubles in seconds

  def update(latencyInMillis: Long): Unit =
    histo.observe(latencyInMillis / 1000.0) // units are doubles in seconds

  def update(latency: Duration): Unit =
    histo.observe(latency.toMillis / 1000.0) // units are doubles in seconds

  def deltaFrom(startTime: DateTime): Long = deltaFrom(startTime.getMillis)

  def deltaFrom(startTimeMillis: Long): Long = {
    val length = System.currentTimeMillis() - startTimeMillis
    update(length)
    length
  }

  override def getType: String = "timer"

}

final case class BasicTimerGauge(name: String, appInfo: AppInfo)(implicit reg: Registry) extends Telemetry with TimerLike {
  val gauge: LabelledGauge = Gauge(toMetricName("timer_gauge_" + name, appInfo), name)
    .labels(label"version", label"type")
    .register
    .labelValues(appInfo.version, getType)

  def time[A](f: => Future[A])(implicit ec: ExecutionContext): Future[A] =
    Time.thisBlock(f).map {
      case (nanos, result) =>
        val secs: Double = nanos.toDouble / (1000.0 * 1000.0)
        println(s"Success[F] - ($nanos)$secs - $result")
        update(nanos)
        result
    }

  def time[A](f: => A): A = {
    val (nanos, result) = Time.thisBlock(f)
    val secs: Double    = nanos.toDouble / (1000.0 * 1000.0)
    println(s"Success[ ] - ($nanos)$secs - $result")
    update(nanos)
    result
  }

  def update(latencyInSeconds: Double): Unit =
    gauge.set(latencyInSeconds) // units are doubles in seconds

  def update(latencyInMillis: Long): Unit =
    gauge.set(latencyInMillis / 1000) // units are doubles in seconds

  def update(latency: Duration): Unit =
    gauge.set(latency.toMillis / 1000) // units are doubles in seconds

  def deltaFrom(startTime: DateTime): Long = deltaFrom(startTime.getMillis)

  def deltaFrom(startTimeMillis: Long): Long = {
    val length = System.currentTimeMillis() - startTimeMillis
    update(length)
    length
  }

  override def getType: String = "gauge"

}

final case class PartitionTrackingGauge(name: String, topicName: String, appInfo: AppInfo)(implicit reg: Registry) extends Telemetry {

  val gauge: TrieMap[Int, LabelledGauge] = TrieMap()

  private def createGauge(name: String, partition: Int) =
    Gauge(toMetricName(s"${name.snakify}_$partition", appInfo), s"$name$partition")
      .labels(label"version", label"type", label"partition")
      .register
      .labelValues(appInfo.version, getType, partition.toString)

  def set(partition: Int, v: Long): Unit = synchronized {
    val g = gauge.getOrElseUpdate(partition, createGauge(name, partition))
    g.set(math.max(g.sum, v))
  }

  def values: Seq[(Int, Double)] =
    gauge
      .map {
        case (k, g) =>
          k -> g.sum
      }
      .toSeq
      .sortBy { case (k, _) => k }

  def asString: String = s"""{"$topicName": [${values.map { case (k, v) => s"{$k: ${v.toLong}}" }.mkString(", ")}]}"""

  def reset(): Unit =
    gauge.clear()

  def getType: String = "gauge"
}

//
// Telemetry Registry
// /////////////////////////////////
object TelemetryRegistry {

  implicit val registry: Registry = DefaultRegistry()

  jmx.register // add in some JVM metrics

  def gauge(name: String)(implicit appInfo: AppInfo): BasicGauge = BasicGauge(name, appInfo)

  def counter(name: String)(implicit appInfo: AppInfo): BasicCounter = BasicCounter(name, appInfo)

  def timer(name: String)(implicit appInfo: AppInfo, buckets: HistogramBuckets = HistogramBuckets(1, 2, 5, 10, 20, 50, 100)): BasicTimer =
    BasicTimer(name, appInfo)

  def timerGauge(name: String)(implicit appInfo: AppInfo) = BasicTimerGauge(name, appInfo)

  def latency(name: String)(implicit appInfo: AppInfo, buckets: HistogramBuckets = HistogramBuckets(1, 2, 5, 10, 20, 50, 100)): BasicTimer =
    BasicTimer(name, appInfo)

  def partitionTracker(name: String, topicName: String)(implicit appInfo: AppInfo): PartitionTrackingGauge =
    PartitionTrackingGauge(name, topicName, appInfo)

  //
  // Initialize Exception Logging
  // ////////////////////////////
  def initializeExceptionLogging(implicit appInfo: AppInfo): Unit = EventTelemetry.initializeExceptionLogging

  //
  // Get Results
  // ////////////////////////////
  def snapshot: scala.Iterator[RegistryMetrics] = registry.collect()

  // DEBUG ONLY
  def debugOutput: String = registry.outputText
}

object EventTelemetry extends Telemetry {

  private val counters: TrieMap[String, LabelledCounter] = new TrieMap[String, LabelledCounter].empty

  def markEvent(eventName: String, tuple: (String, String))(implicit appInfo: AppInfo): Unit = {

    def genKey(): String = eventName.snakeClassname

    def createCounter(): LabelledCounter =
      Counter(toMetricName(genKey(), appInfo), eventName)
        .labels(label"version", label"type", label"event", LabelName(tuple._1))
        .register(TelemetryRegistry.registry)
        .labelValues(appInfo.version, getType, "mark", tuple._2)

    counters.getOrElse(genKey(), createCounter()).inc()
  }

  def initializeExceptionLogging(implicit app: AppInfo, registry: Registry): Unit = {
    val appender: AppenderBase[ILoggingEvent] = new AppenderBase[ILoggingEvent] {

      def genKey(element: StackTraceElement): String = element.getClassName.snakeClassname

      def createCounter(element: StackTraceElement, event: ILoggingEvent): LabelledCounter =
        Counter(MetricName(genKey(element)), element.getClassName)
          .labels(label"version", label"exception", label"line_number", label"type", label"event")
          .register
          .labelValues(app.version, event.getThrowableProxy.getClassName, element.getLineNumber.toString, getType, "exception")

      override def append(event: ILoggingEvent): Unit =
        event.getCallerData
          .find(_.getClassName.contains("me.lightspeed7.sk8s"))
          .foreach { element =>
            counters.getOrElse(genKey(element), createCounter(element, event)).inc()
          }
    }

    appender.setContext(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext])
    appender.start()

    LoggerFactory
      .getLogger(Logger.ROOT_LOGGER_NAME)
      .asInstanceOf[ch.qos.logback.classic.Logger]
      .addAppender(appender)
  }

  override def getType: String = "counter"

}
