package me.lightspeed7.sk8s.util

import java.util.UUID

import me.lightspeed7.sk8s.telemetry.TimerLike

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object String {

  implicit class StringPimps(s: String) {

    def notEmpty: Option[String] =
      s match {
        case "" => None
        case _  => Option(s)
      }

    def notBlank: Option[String] = s.notEmpty.flatMap(_ => s.trim.notEmpty)

    def toBytes: Option[Array[Byte]] = s.notEmpty.flatMap(s => s.toBytes)

    def notNull: String = if (s == null) "" else s

    object pad {

      private def doPad(padChar: Char, length: Int) = padChar.toString * (length - notNull.length)

      object left {
        def apply(padChar: Char, length: Int): String = doPad(padChar, length) + notNull
      }

      object right {
        def apply(padChar: Char, length: Int): String = notNull + doPad(padChar, length)
      }

    }

    def tryParseUUID: Option[UUID] = Try(UUID.fromString(s)).toOption // not the best method to have

    /**
      * Remove all the characters from a string exception a-z, A-Z, 0-9, and '_'
      *
      * @return the cleaned string and an empty string if the input is null
      */
    def clean: String = s.notNull.replaceAll("[^a-zA-Z0-9_]", "")

    /**
      * Turn a string of format "FooBar" into snake case "foo_bar"
      *
      * Note: snakify is not reversible
      *
      * @return the underscored string
      */
    def snakify: String =
      s.notNull.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2").replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase

    /**
      * Turn a string of format "FooBar" into snake case "foo-bar"
      *
      * Note: dashify is not reversible
      *
      * @return the dashed string
      */
    def dashify: String =
      s.notNull.replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2").replaceAll("([a-z\\d])([A-Z])", "$1-$2").toLowerCase

    /**
      * Parse a string and return the Long value of that string.<p/>
      * The string can start with '-' if it is a negative number or '+' for a positive number
      *
      * @return the Long value of the input String
      */
    def parseNumber: Long = {
      def cToL(in: Char) = in.toLong - '0'.toLong

      def p(in: List[Char]) = in.takeWhile(Character.isDigit).foldLeft(0L)((acc, c) => (acc * 10L) + cToL(c))

      s.notNull.trim.toList match {
        case '-' :: xs => -p(xs)
        case '+' :: xs => p(xs)
        case xs        => p(xs)
      }
    }

    /**
      * Add commas before the last 3 characters
      *
      * @return the string with commas
      */
    def commafy: String = {
      def commaIt(in: List[Char]): List[Char] =
        in match {
          case Nil                  => in
          case _ :: Nil             => in
          case _ :: _ :: Nil        => in
          case _ :: _ :: _ :: Nil   => in
          case x1 :: x2 :: x3 :: xs => x1 :: x2 :: x3 :: ',' :: commaIt(xs)
        }

      commaIt(s.notNull.toList.reverse).reverse.mkString("")
    }

    def stripQuotes: String = s.replaceAll("^[\"']|[\"']$", "")

    def stripBracing: String = s.replaceAll("^[({\\[]|[)}\\]]$", "")
  }
}

object PrettyPrint {

  private val units  = Array[String]("B", "K", "M", "G", "T")
  private val format = new java.text.DecimalFormat("#,##0.#")

  def fileSizing(input: Long): String = {
    if (input <= 0) return "0.0"
    val digitGroups = (Math.log10(input.toDouble) / Math.log10(1024)).floor.toInt
    format.format(input / Math.pow(1024.0, digitGroups.toDouble)) + " " + units(digitGroups)
  }

  def number(input: Long): String = format.format(input)

  def latency(timeInNanos: Long): String = {
    val micro        = timeInNanos / 1000
    val millis       = micro / 1000
    val rawSecs: Int = (millis.toDouble / 1000).floor.toInt
    val mins: Int    = rawSecs / 60
    val secs: Int    = rawSecs - (mins * 60)

    (mins, secs, millis) match {
      case (m, s, _) if m > 0 => s"$m mins $s seconds"
      case (_, s, _) if s > 0 => s"$s seconds"
      case (_, _, m) if m > 0 => s"$millis milliseconds"
      case (_, _, _)          => s"$micro microseconds"
    }
  }
}

object Time {

  /**
    * Time the block of code in nanos
    *
    * @param block - defined as call-by-name
    * @tparam T - the type of result
    * @return nano time for the block of code
    */
  def thisBlock[T](block: => T): (Long, T) = {
    val t0     = System.nanoTime()
    val result = block // call-by-name
    val t1     = System.nanoTime()
    (t1 - t0, result)
  }

  def thisBlock[T](block: => Future[T])(implicit ec: ExecutionContext): Future[(Long, T)] = {
    val t0 = System.nanoTime()
    block.map { out => // call-by-name
      val t1 = System.nanoTime()
      (t1 - t0, out)
    }
  }

  trait TimerOutput {
    def update(latencyInMillis: Long, count: Int): Unit
  }

  final case class TimeItTimer(timer: TimerLike) extends TimerOutput {
    override def update(latencyInMillis: Long, count: Int): Unit = timer.update(latencyInMillis) // ignores count
  }

  def it[T](label: => String, output: String => Unit = in => println(in))(
    block: => T
  )(implicit timer: Option[TimerOutput] = None): T = {
    // run it
    output(s"$label - Starting  ...")
    val (time, result) = thisBlock(block) // call-by-name
    val count = result match {
      case seq: Seq[_] => seq.length
      case _           => 1
    }

    timer.foreach(_.update(time, count))
    output(s"$label - Elapsed Time: " + PrettyPrint.latency(time))
    result
  }

  def repeated[T](label: => String, runs: Int = 1, output: String => Unit = in => println(in))(block: => T): Seq[T] =
    it(label, output) {
      (1 to runs).map { _ =>
        block
      }
    }

}
