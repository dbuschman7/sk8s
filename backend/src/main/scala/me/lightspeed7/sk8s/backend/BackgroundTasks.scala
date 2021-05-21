package me.lightspeed7.sk8s.backend

import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.Sk8s.HealthStatus
import me.lightspeed7.sk8s._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class BackgroundTasks(implicit val ctx: Sk8sContext) extends LazyLogging {
  import scala.language.postfixOps
  import ctx._

  logger.info("Background tasks started")

  // log memory for app periodically, default is every 30 seconds,
  lazy val memoryUpdate: FiniteDuration =
    Variables.source[FiniteDuration](Sources.env, "MEMORY_CRON_INTERVAL", Constant(30 seconds)).value

  lazy val startServer: Boolean = Variables
    .firstValue[Boolean](
      BackgroundTasks.ServerStartName,
      Variables.maybeSource(Sources.env, BackgroundTasks.ServerStartName),
      Constant(true)
    )
    .value

  lazy val bindAddress: String = Variables
    .firstValue[String](
      BackgroundTasks.ServerAddressName,
      Variables.maybeSource(Sources.env, BackgroundTasks.ServerAddressName),
      Constant("0.0.0.0") //
    )
    .value

  lazy val bindPort: Int = Variables
    .firstValue[Int](
      BackgroundTasks.ServerAddressPort,
      Variables.maybeSource(Sources.env, BackgroundTasks.ServerAddressPort),
      Constant(8999) //
    )
    .value

  Sk8s.MemoryCron.startup(appInfo, memoryUpdate)

  if (startServer) {
    logger.info("Staring backend server ...")
    Try(new BackendServer(bindAddress, bindPort)(ctx)) match {
      case Failure(th: Throwable) =>
        logger.error("", th)
        HealthStatus.unhealthy("backend-server")
      case Success(_) => // just ignore
    }
  }

  logger.info("Background tasks started")

}

object BackgroundTasks {
  val ServerStartName: String = "BACKEND_SERVER"
  val ServerAddressName       = "BACKEND_BIND_ADDRESS"
  val ServerAddressPort       = "BACKEND_BIND_PORT"

  var singleton: Option[BackgroundTasks] = None

  def standup(implicit ctx: Sk8sContext): BackgroundTasks =
    synchronized {
      if (singleton.isEmpty) {
        val bTasks = new BackgroundTasks()
        singleton = Some(bTasks)
      }
      singleton.get
    }
}
