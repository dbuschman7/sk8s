package me.lightspeed7.sk8s

import java.io.File
import java.nio.file.{ Path, Paths }
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ FileIO, Framing, Sink }
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings, Supervision }
import akka.util.ByteString
import me.lightspeed7.sk8s.util.AutoClose
import org.joda.time.{ DateTime, DateTimeZone }
import org.scalactic.source
import org.scalatest.{ BeforeAndAfterAll, FunSuite, Tag }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class Sk8sFunSuite extends FunSuite with BeforeAndAfterAll {

  RunMode.setTestRunMode()

  val now: DateTime = {
    DateTimeZone.setDefault(DateTimeZone.UTC)
    DateTime.now
  }

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val appInfo: AppInfo     = AppInfo(this.getClass.getName, "0.0.0", now)

  lazy val actorSystemName: String = UUID.randomUUID().toString

  implicit val rm: RunMode = ctx.runMode

  implicit lazy val actorSystem: ActorSystem = {
    println(s"Starting up ActorSystem '$actorSystemName' ...")
    ActorSystem(actorSystemName)

  }

  private val decider: Supervision.Decider = { t =>
    println("exception during graph, stopping" + t)
    t.printStackTrace()
    Supervision.Stop
  }

  implicit lazy val materializer: ActorMaterializer = {
    println("Starting up ActorMaterializer ...")
    ActorMaterializer(ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider))
  }

  override def beforeAll: Unit =
    super.beforeAll()

  override def afterAll(): Unit = {
    println("Closing AutoCloseables ...")
    closeables.reverse // opposite of registration
      .foreach {
        case (l, c) =>
          println(s"Closing - $l")
          c.close()
      }
    println("Shutting down ActorMaterializer ...")
    materializer.shutdown()
    Await.result(actorSystem.terminate(), Duration.Inf)
    ctx.close()
  }

  implicit lazy val ctx: Sk8sContext = Sk8sContext(appInfo)

  lazy val k8sActive: Boolean = Sk8s.isKubernetes()

  implicit val timeout: FiniteDuration = 10 seconds

  private var closeables: Seq[(String, AutoCloseable)] = Seq.empty

  def registerCloseable[T <: AutoCloseable](toClose: T)(label: String = toClose.getClass.getName): T = {
    println(s"Register Closeable - $label")
    closeables = closeables :+ (label, toClose)
    toClose
  }

  //  @deprecated("User the .result augmentation instead", "2018-03-01")
  def await[T](f: Future[T]): T = Await.result(f, timeout)

  //  @deprecated("User the .ready augmentation instead", "2018-03-01")
  def awaitCatch[T](f: Future[T]): Try[T] = Await.ready[T](f, timeout).value.get

  //
  protected implicit class unitHelper[T](item: T) {
    def toUnit: Unit = ()

    def toBlackHole: Unit = toUnit
  }

  //
  protected implicit class futureHelper[T](item: Future[T])(implicit t: FiniteDuration) {

    def toValue(implicit timeout: FiniteDuration = t): T = Await.result(item, timeout)

    def toValueTry(implicit timeout: FiniteDuration = t): Try[T] = Await.ready[T](item, timeout).value.get

    def resultOrFail(implicit timeout: FiniteDuration = t): T = Await.ready[T](item, timeout).value match {
      case Some(Success(v))  => v
      case Some(Failure(ex)) => fail(ex.getMessage, ex)
      case None              => fail("Future failed to complete")
    }

    def toUnit: Unit = {
      Await.result(item, timeout)
      ()
    }

    def toBlackHole: Unit = toUnit
  }

  protected def runModeContext[T](runMode: RunMode): AutoClose[AutoCloseable] = {

    val current: RunMode = RunMode.currentRunMode

    RunMode.setRunMode(runMode)

    AutoClose(new AutoCloseable {
      override def close(): Unit = RunMode.setRunMode(current)
    })
  }

  //
  // File-based helpers
  // ///////////////////////////////
  def findTreeFiles(keyWords: String*): Array[File] = findTreeFiles(Paths.get(".").toAbsolutePath.toFile, keyWords: _*)

  def findTreeFiles(baseDir: File, keyWords: String*): Array[File] = {
    val these = baseDir.listFiles
    val good = these.filter { f =>
      val fStr = f.toString
      keyWords.forall(kw => fStr.contains(kw))
    }
    good ++ these.filter(_.isDirectory).flatMap(findTreeFiles(_, keyWords: _*))
  }

  def getProjectBasePath(project: String): Path = {
    val t = Paths.get(".").toAbsolutePath.toString.replace("/.", "")
    if (t.contains(project)) Paths.get(t) else Paths.get(t, project)
  }

  def getFileContents(file: File): ByteString = await(FileIO.fromFile(file).runFold(ByteString.empty)(_ ++ _))

  def getFileContents(path: Path): ByteString = await(FileIO.fromPath(path).runFold(ByteString.empty)(_ ++ _))

  def getPathContents(path: Path): ByteString = await(FileIO.fromPath(path).runFold(ByteString.empty)(_ ++ _))

  def getPathLines(path: Path, maxLineLength: Int = 10000): Seq[String] = getFileLines(path.toFile)

  def getFileLines(file: File, maxLineLength: Int = 10000): Seq[String] = {
    val f = FileIO
      .fromFile(file)
      .via(Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = maxLineLength, allowTruncation = true))
      .map(_.utf8String)
      .runWith(Sink.seq)

    await(f)
  }

  def getProjectFilePath(project: String, pathParts: String*): Path =
    Paths.get(getProjectBasePath(project).toString, pathParts.mkString("/")).toAbsolutePath

  def getPlayTestFilePath(project: String, pathParts: String*): Path = {
    val parts: Seq[String] = Seq("test", "resources") ++ pathParts
    Paths.get(getProjectBasePath(project).toString, parts.mkString("/")).toAbsolutePath
  }

  def getLibraryTestFilePath(project: String, pathParts: String*): Path = {
    val parts: Seq[String] = Seq("src", "test", "resources") ++ pathParts
    Paths.get(getProjectBasePath(project).toString, parts.mkString("/")).toAbsolutePath
  }

  //
  // Run this test only if we are in the K8S environment
  // ///////////////////////////////////////////////////////////
  protected def testIfK8s(testName: String, testTags: Tag*)(testFun: => Any /* Assertion */ )(implicit pos: source.Position): Unit =
    if (k8sActive) test(testName, testTags: _*)(testFun)(pos)
    else ignore(testName + " !!! K8s REQUIRED ", testTags: _*)(testFun)(pos)

  protected def testIfNotK8s(testName: String, testTags: Tag*)(testFun: => Any /* Assertion */ )(implicit pos: source.Position): Unit =
    if (!k8sActive) test(testName, testTags: _*)(testFun)(pos)
    else ignore(testName + " !!! K8s REQUIRED ", testTags: _*)(testFun)(pos)
}
