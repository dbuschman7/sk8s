package me.lightspeed7.sk8s.jfrog

import java.io.File
import java.nio.file.Paths

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ HttpRequest, Uri }
import akka.stream.Materializer
import akka.stream.scaladsl.{ FileIO, Framing, Sink }
import akka.util.ByteString
import me.lightspeed7.sk8s.Sk8sContext

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.xml.{ Elem, NodeSeq, XML }

case class Creds(username: String, password: String)

object ArtifactoryCreds {
  def parse(in: String): Option[Creds] = {
    val parts = Option(in)
      .getOrElse("")
      .replace("(", "")
      .replace(")", "")
      .replace("\"", "")
      .split(",")
      .map(_.trim)
      .reverse
      .take(2)

    if (parts.length == 2) {
      Option(Creds(parts(1), parts(0)))
    } else {
      None
    }
  }

  import scala.util.matching.Regex

  def recursiveListFiles(f: File, r: Regex): Array[File] = {
    val these = f.listFiles
    val good  = these.filter(f => r.findFirstIn(f.getName).isDefined)
    good ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_, r))
  }

  def getHomeDirCreds(implicit ec: ExecutionContext, mat: Materializer): Option[Creds] = {

    val dataLines: Seq[String] = sys.env
      .get("HOME")
      .map { home: String =>
        Seq("1.0", "0.13").map(v => s"$home/.sbt/$v")
      }
      .getOrElse(Seq())
      .flatMap { dir: String =>
        println("Testing dir - " + dir)
        recursiveListFiles(Paths.get(dir).toFile, """.*\.sbt$""".r).flatMap { file: File =>
          fileLines(file)
            .filter(l => l.startsWith("credentials += ") && l.contains("symphonyai.jfrog.io"))
        }.toSeq
      }

    dataLines.headOption.flatMap(parse)
  }

  implicit val timeout: FiniteDuration = 10 seconds

  def await[T](f: Future[T]): T = Await.result(f, timeout)

  def fileContents(file: File)(implicit mat: Materializer): ByteString =
    await(FileIO.fromPath(file.toPath).runFold(ByteString.empty)(_ ++ _))

  def fileLines(file: File, maxLineLength: Int = 10000)(implicit mat: Materializer): Seq[String] = {
    val f = FileIO
      .fromPath(file.toPath)
      .via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = maxLineLength, allowTruncation = true))
      .map(_.utf8String)
      .runWith(Sink.seq)
    await(f)
  }
}

final case class Artifactory(username: String, password: String)(implicit appCtx: Sk8sContext) {

  import appCtx._

  def fetchVersions(groupId: String, artifactId: String, repo: String = "symphony-local"): Future[Seq[String]] = {

    import me.lightspeed7.sk8s.util.String._

    val getUrl =
      s"""https://symphonyai.jfrog.io/symphonyai/$repo/${groupId.replace(".", "/")}/$artifactId/maven-metadata.xml"""
    val request = HttpRequest(uri = Uri(getUrl)).addCredentials(BasicHttpCredentials(username, password))

    Http().singleRequest(request).flatMap {
      case response if response.status.intValue == 200 =>
        response.entity.toStrict(10 seconds).map { entity =>
          val body: String      = entity.data.decodeString("UTF-8")
          val xml: Elem         = XML.loadString(body)
          val versions: NodeSeq = xml \ "versioning" \ "versions"
          versions.theSeq.flatMap(_.child.flatMap(_.text.notBlank))
        }
      case _ => Future successful Seq[String]()
    }
  }

}
