package io.timeli.sk8s.util

import java.nio.file.{ Path, Paths, StandardOpenOption }

import com.typesafe.scalalogging.LazyLogging
import io.timeli.sk8s.telemetry.{ BasicTimer, BasicTimerGauge, TimerLike }

import scala.concurrent.{ ExecutionContext, Future }
import scala.io.Source
import scala.util.{ Failure, Success, Try }

trait FileUtils extends LazyLogging {
  def getContents(basePath: Path, filename: String): Option[String] = getContents(Paths.get(basePath.toString, filename))

  def getContents(fullPath: Path): Option[String] = {
    logger.debug(s"getContents - path = $fullPath")
    // use new Java nio parts to get a read-only access files read correctly.
    Try(Source.fromInputStream(java.nio.file.Files.newInputStream(fullPath, StandardOpenOption.READ), "UTF-8").mkString) match {
      case Success(value) => Some(value)
      case Failure(ex) =>
        logger.warn(s"Unable to fetch $fullPath - ${ex.getMessage}")
        None
    }
  }

  def exec(command: String): String = {
    import sys.process._
    command !!
  }

}

object PrettyPrint {

  private val units = Array[String]("B", "K", "M", "G", "T")
  private val format = new java.text.DecimalFormat("#,##0.#")

  def fileSizing(input: Long): String = {
    if (input <= 0) return "0.0"
    val digitGroups = (Math.log10(input.toDouble) / Math.log10(1024)).floor.toInt
    format.format(input / Math.pow(1024.0, digitGroups.toDouble)) + " " + units(digitGroups)
  }

  def number(input: Long): String = format.format(input)

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
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    (t1 - t0, result)
  }

  def thisBlock[T](block: => Future[T])(implicit ec: ExecutionContext): Future[(Long, T)] = {
    val t0 = System.nanoTime()
    block.map { out => // call-by-name
      val t1 = System.nanoTime()
      (t1 - t0, out)
    }
  }

  final case class TimeItTimer(timer: TimerLike)

  def it[T](label: => String, output: String => Unit = in => println(in))(block: => T)(implicit timer: Option[TimeItTimer] = None): T = {
    // run it
    output(s"$label - Starting  ...")
    val (time, result) = thisBlock(block) // call-by-name
    timer.foreach(_.timer.update(time))

    // dump the results
    val micro = time / 1000
    val millis = micro / 1000
    val rawSecs: Int = (millis.toDouble / 1000).floor.toInt
    val mins: Int = rawSecs / 60
    val secs: Int = rawSecs - (mins * 60)

    (mins, secs, millis) match {
      case (m, s, _) if m > 0 => output(s"$label - Elapsed Time: $m mins $s seconds")
      case (_, s, _) if s > 0 => output(s"$label - Elapsed Time: $s seconds")
      case (_, _, m) if m > 0 => output(s"$label - Elapsed Time: $millis milliseconds")
      case (_, _, _)          => output(s"$label - Elapsed Time: $micro microseconds")
    }

    result
  }

  def repeated[T](label: => String, runs: Int = 1, output: String => Unit = in => println(in))(block: => T): Seq[T] = it(label, output) {
    (1 to runs).map { _ => block }
  }

}

