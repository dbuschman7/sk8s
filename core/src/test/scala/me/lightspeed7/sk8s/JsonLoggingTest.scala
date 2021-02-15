package me.lightspeed7.sk8s

import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, LoggerContext}
import ch.qos.logback.core.AppenderBase
import me.lightspeed7.sk8s.logging.LazyJsonLogging
import org.scalatest.BeforeAndAfterAll
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

import scala.collection.mutable
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// Helpers
final case class Foo(bar: String, baz: Int)

object Foo {
  implicit val _json = Json.format[Foo]
}

class JsonLoggingTest extends AnyFunSuite with BeforeAndAfterAll with Matchers with LazyJsonLogging {

  val context  = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
  val appender = new MapAppender

  override def beforeAll() = {
    val root = context.getLogger(Logger.ROOT_LOGGER_NAME)
    root.setLevel(Level.INFO)

    val warningFilter = new ThresholdFilter
    warningFilter.setLevel(Level.INFO.toString)
    warningFilter.setContext(context)
    warningFilter.start()
    appender.addFilter(warningFilter)
    appender.start()
    root.addAppender(appender)

  }

  override def afterAll() =
    appender.stop()

  test("Test Json Logging") {
    logger.Json.info(Foo("bar1", 123))
    logger.Json.warn(Foo("bar2", 234))
    logger.Json.trace(Foo("bar3", 345))

    appender.events.map(println)
    // results
    appender.events.size should be >= 2
    appender.events.map(_.getLevel).toSet shouldBe Set(Level.INFO, Level.WARN)
    appender.events.count(_.getMessage.endsWith("}")) should be >= 2
    val data = appender.events.map(_.getMessage()).mkString(",")
    data.contains("""{"bar":"bar1","baz":123}""") shouldBe true
    data.contains("""{"bar":"bar2","baz":234}""") shouldBe true

  }

}

// custom appender
class MapAppender extends AppenderBase[ILoggingEvent] {
  var events = new mutable.ListBuffer[ILoggingEvent]()

  override protected def append(event: ILoggingEvent): Unit =
    events += event

}
