package me.lightspeed7.sk8s.files

import java.io.File
import java.nio.file.{Path, Paths, StandardOpenOption}

import akka.stream
import akka.stream.{Materializer, scaladsl}
import akka.stream.scaladsl.{Framing, Sink}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.{Failure, Success, Try}

//
// File based helpers
// ////////////////////////
object Sk8sFileIO extends LazyLogging {

  def getContents(basePath: Path, filename: String): Option[String] =
    getContents(Paths.get(basePath.toString, filename))

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

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit): Unit = {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def writeContents(fullPath: Path)(data: String*): Unit =
    printToFile(fullPath.toFile) { p =>
      data.foreach(p.println)
    }

  //
  def await[T](f: Future[T])(implicit timeout: FiniteDuration): T = Await.result(f, timeout)

  //
  def findTreeFiles(baseDir: File, keyWords: String*): Array[File] = {
    val these = baseDir.listFiles
    val good = these.filter { f =>
      val fStr = f.toString
      keyWords.forall(kw => fStr.contains(kw))
    }
    good ++ these.filter(_.isDirectory).flatMap(findTreeFiles(_, keyWords: _*))
  }

  //
  def getFileContents(file: File)(implicit timeout: FiniteDuration, mat:Materializer): ByteString = await(stream.scaladsl.FileIO.fromPath(file.toPath).runFold(ByteString.empty)(_ ++ _))

  def getFileContents(path: Path)(implicit timeout: FiniteDuration, mat:Materializer): ByteString = await(scaladsl.FileIO.fromPath(path).runFold(ByteString.empty)(_ ++ _))

  def getPathContents(path: Path)(implicit timeout: FiniteDuration, mat:Materializer): ByteString = await(scaladsl.FileIO.fromPath(path).runFold(ByteString.empty)(_ ++ _))

  def getPathLines(path: Path, maxLineLength: Int = 10000)(implicit timeout: FiniteDuration, mat:Materializer): Seq[String] = getFileLines(path.toFile)

  def getFileLines(file: File, maxLineLength: Int = 10000)(implicit timeout: FiniteDuration, mat:Materializer): Seq[String] = {
    val f = scaladsl.FileIO
      .fromPath(file.toPath)
      .via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = maxLineLength, allowTruncation = true))
      .map(_.utf8String)
      .runWith(Sink.seq)

    await(f)
  }
}
