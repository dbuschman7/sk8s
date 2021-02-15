package me.lightspeed7.sk8s.logging

import com.typesafe.scalalogging.{Logger => Underlying}
import org.slf4j.Marker
import play.api.libs.json.{JsValue, Writes}

import scala.reflect.ClassTag

/**
  * Defines `logger` as a lazy value initialized with an underlying `org.slf4j.Logger`
  * named according to the class into which this trait is mixed.
  */
trait LazyJsonLogging {

  protected lazy val logger: Logger = Logger.apply(getClass.getName)
}

/**
  * Defines `logger` as a value initialized with an underlying `org.slf4j.Logger`
  * named according to the class into which this trait is mixed.
  */
trait StrictJsonLogging {

  protected val logger: Logger = Logger.apply(getClass.getName)

}

object Logger {

  /**
    * Create a [[Logger]] wrapping the given underlying `org.slf4j.Logger`.
    */
  def apply(underlying: Underlying): Logger = new Logger(underlying)

  /**
    * Create a [[Logger]] for the given name.
    * Example:
    * {{{
    *   val logger = Logger("application")
    * }}}
    */
  def apply(name: String): Logger = new Logger(Underlying(name))

  /**
    * Create a [[Logger]] wrapping the created underlying `org.slf4j.Logger`.
    */
  def apply(clazz: Class[_]): Logger = new Logger(Underlying(clazz.getName))

  /**
    * Create a [[Logger]] for the runtime class wrapped by the implicit class
    * tag parameter.
    * Example:
    * {{{
    *   val logger = Logger[MyClass]
    * }}}
    */
  def apply[T](implicit ct: ClassTag[T]): Logger = new Logger(Underlying(ct.runtimeClass.getName.stripSuffix("$")))

}

@SerialVersionUID(7538248225L)
final class Logger private (val underlying: Underlying) extends Serializable {

  object Json {

    //
    // Json Handlers
    // ////////////////////////

    def error[T](obj: T)(implicit _writes: Writes[T]): Unit = error(play.api.libs.json.Json.toJson(obj))

    def info[T](obj: T)(implicit _writes: Writes[T]): Unit = info(play.api.libs.json.Json.toJson(obj))

    def warn[T](obj: T)(implicit _writes: Writes[T]): Unit = warn(play.api.libs.json.Json.toJson(obj))

    def debug[T](obj: T)(implicit _writes: Writes[T]): Unit = debug(play.api.libs.json.Json.toJson(obj))

    def trace[T](obj: T)(implicit _writes: Writes[T]): Unit = trace(play.api.libs.json.Json.toJson(obj))

    //
    // JsValue methods
    // ////////////////////////
    def error(json: => JsValue): Unit =
      if (underlying.underlying.isErrorEnabled) {
        underlying.error(json.toString)
      }

    def error(json: => JsValue, message: => String): Unit =
      if (underlying.underlying.isErrorEnabled) {
        underlying.error(message + " " + json.toString)
      }

    def warn(json: => JsValue): Unit =
      if (underlying.underlying.isWarnEnabled()) {
        underlying.warn(json.toString)
      }

    def warn(json: => JsValue, message: => String): Unit =
      if (underlying.underlying.isWarnEnabled()) {
        underlying.warn(message + " " + json.toString)
      }

    def info(json: => JsValue): Unit =
      if (underlying.underlying.isInfoEnabled()) {
        underlying.info(json.toString)
      }

    def info(json: => JsValue, message: => String): Unit =
      if (underlying.underlying.isInfoEnabled()) {
        underlying.info(message + " " + json.toString)
      }

    def debug(json: => JsValue): Unit =
      if (underlying.underlying.isDebugEnabled()) {
        underlying.debug(json.toString)
      }

    def debug(json: => JsValue, message: => String): Unit =
      if (underlying.underlying.isDebugEnabled()) {
        underlying.debug(message + " " + json.toString)
      }

    def trace(json: => JsValue): Unit =
      if (underlying.underlying.isTraceEnabled()) {
        underlying.trace(json.toString)
      }

    def trace(json: => JsValue, message: => String): Unit =
      if (underlying.underlying.isTraceEnabled()) {
        underlying.trace(message + " " + json.toString)
      }

  }

  //
  // Pass-Thru Methods
  // /////////////////////////////////
  //
  // Error
  def error(message: => String): Unit = underlying.error(message)

  def error(message: => String, cause: Throwable): Unit = underlying.error(message, cause)

  def error(message: => String, args: Any*): Unit = underlying.error(message, args)

  def error(marker: Marker, message: => String): Unit = underlying.error(marker, message)

  def error(marker: Marker, message: => String, cause: Throwable): Unit = underlying.error(marker, message, cause)

  def error(marker: Marker, message: => String, args: Any*): Unit = underlying.error(marker, message, args)

  // Warn
  def warn(message: => String): Unit = underlying.warn(message)

  def warn(message: => String, cause: Throwable): Unit = underlying.warn(message, cause)

  def warn(message: => String, args: Any*): Unit = underlying.warn(message, args)

  def warn(marker: Marker, message: => String): Unit = underlying.warn(marker, message)

  def warn(marker: Marker, message: => String, cause: Throwable): Unit = underlying.warn(marker, message, cause)

  def warn(marker: Marker, message: => String, args: Any*): Unit = underlying.warn(marker, message, args)

  // Info
  def info(message: => String): Unit = underlying.info(message)

  def info(message: => String, cause: Throwable): Unit = underlying.info(message, cause)

  def info(message: => String, args: Any*): Unit = underlying.info(message, args)

  def info(marker: Marker, message: => String): Unit = underlying.info(marker, message)

  def info(marker: Marker, message: => String, cause: Throwable): Unit = underlying.info(marker, message, cause)

  def info(marker: Marker, message: => String, args: Any*): Unit = underlying.info(marker, message, args)

  // Debug
  def debug(message: => String): Unit = underlying.debug(message)

  def debug(message: => String, cause: Throwable): Unit = underlying.debug(message, cause)

  def debug(message: => String, args: Any*): Unit = underlying.debug(message, args)

  def debug(marker: Marker, message: => String): Unit = underlying.debug(marker, message)

  def debug(marker: Marker, message: => String, cause: Throwable): Unit = underlying.debug(marker, message, cause)

  def debug(marker: Marker, message: => String, args: Any*): Unit = underlying.debug(marker, message, args)

  // Trace
  def trace(message: => String): Unit = underlying.trace(message)

  def trace(message: => String, cause: Throwable): Unit = underlying.trace(message, cause)

  def trace(message: => String, args: Any*): Unit = underlying.trace(message, args)

  def trace(marker: Marker, message: => String): Unit = underlying.trace(marker, message)

  def trace(marker: Marker, message: => String, cause: Throwable): Unit = underlying.trace(marker, message, cause)

  def trace(marker: Marker, message: => String, args: Any*): Unit = underlying.trace(marker, message, args)

}
