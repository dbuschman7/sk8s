package me.lightspeed7.sk8s.server

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
import me.lightspeed7.sk8s.AppInfo
import me.lightspeed7.sk8s.logging.LazyJsonLogging
import me.lightspeed7.sk8s.util.PrettyPrint
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class MemoryCronActor(application: AppInfo, delay: FiniteDuration, interval: FiniteDuration) extends Actor with LazyJsonLogging {
  import scala.language.postfixOps
  import MemoryCronActor._

  protected lazy val actorClassName: String = this.getClass.getName

  object Ping

  implicit val ec: ExecutionContextExecutor = this.context.dispatcher

  registerCancelable(delay, interval, Ping)

  def pretty(in: Long): String = PrettyPrint.fileSizing(in)

  def receive: Receive = {
    case Ping =>
      val info = MemoryInfo.create()
      logger.Json.info(generateJson(info))
  }

  private var cancellable: Cancellable = _

  def registerCancelable(delay: FiniteDuration, interval: FiniteDuration, token: Any)(implicit ec: ExecutionContext): Unit = {
    val cronTokenName = token.getClass.getName
    logger.info(
      s"Starting Memory Cron -> Delay = ${delay.toString} Interval = ${interval.toString} with token of type $cronTokenName"
    )
    cancellable = this.context.system.scheduler.schedule(delay, interval, self, token)
  }

  // setup
  override def preStart() {
    super.preStart()
  }

  // teardown
  override def postStop() {
    logger.info("Memory Cron Stopping")
    if (cancellable != null) {
      cancellable.cancel
    }
    super.postStop()
  }

}

object MemoryCronActor extends LazyJsonLogging {
  import scala.language.postfixOps
  import scala.concurrent.duration._

  def pretty(in: Long): String = PrettyPrint.fileSizing(in)

  def generateJson(info: MemoryInfo): JsObject =
    Json.obj(
      "sk8s"       -> "memory",
      "processors" -> info.processors,
      //
      "total"   -> pretty(info.total),
      "totalMB" -> info.total / (1024 * 1024),
      //
      "free"   -> pretty(info.free),
      "freeMB" -> info.free / (1024 * 1024),
      //
      "max"   -> pretty(info.max),
      "maxMB" -> info.max / (1024 * 1024),
      //
      "percent" -> info.percent //
    )

  def startup(appInfo: AppInfo, interval: Duration = 30 seconds)(implicit akka: ActorSystem): ActorRef = {
    logger.info("Activating MemoryCronActor")
    akka.actorOf(Props(classOf[MemoryCronActor], appInfo, interval, interval))
  }
}

case class MemoryInfo(processors: Int, free: Long, max: Long, total: Long) {
  lazy val used: Long    = total - free
  lazy val percent: Long = used * 100 / max

  def toJson: JsObject =
    Json.obj(
      "sk8s"       -> "memory",
      "processors" -> this.processors,
      //
      "total" -> this.total,
      "free"  -> this.free,
      "max"   -> this.max,
      //
      "percentUsed" -> this.percent //
    )

}

object MemoryInfo {
  val rt: Runtime = Runtime.getRuntime

  def create(): MemoryInfo = MemoryInfo(rt.availableProcessors(), rt.freeMemory(), rt.maxMemory(), rt.totalMemory())
}
