package me.lightspeed7.sk8s.util

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import me.lightspeed7.sk8s.AppInfo
import me.lightspeed7.sk8s.telemetry.{ BackendServer, BackendServerClient }
import org.joda.time.DateTime
import org.scalatest.{ BeforeAndAfterAll, FunSuite, Matchers }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.Try

class AsyncRunnerTest extends FunSuite with BeforeAndAfterAll with Matchers {

  implicit val akka: ActorSystem    = ActorSystem("Prometheus")
  implicit val ec: ExecutionContext = akka.dispatcher

  implicit val appInfo: AppInfo = AppInfo("name", "version", DateTime.now)

  val export = new BackendServer(protobufFormet = false, configGen = "Config")
  val client = BackendServerClient()

  override def afterAll(): Unit = {
    client.close()
    Try(Closeables.close()) // just ignore
    Await.result(akka.terminate(), Duration.Inf)
  }

  test("Run multiple times test") {

    val atomic = new AtomicInteger(0)

    AsyncRunner.runFor(5 seconds, 500 milliseconds) {
      atomic.incrementAndGet()
      ()
    }

    Thread.sleep((6 seconds).toMillis)

    println(s"Iterations = ${atomic.get()}")

    atomic.get() should be > 0
    atomic.get() should be < 11
  }

}
