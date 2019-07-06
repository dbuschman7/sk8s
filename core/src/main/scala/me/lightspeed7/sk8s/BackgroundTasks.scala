package me.lightspeed7.sk8s

import com.typesafe.scalalogging.LazyLogging
import javax.inject.{ Inject, Singleton }
import me.lightspeed7.sk8s.Sk8s.HealthStatus
import me.lightspeed7.sk8s.telemetry.BackendServer

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

@Singleton
class BackgroundTasks @Inject()(implicit val ctx: Sk8sContext) extends LazyLogging {

  import ctx._

  logger.info("Background tasks started")

  // log memory for app periodically, default is every 30 seconds,
  lazy val memoryUpdate: FiniteDuration =
    Variables.source[FiniteDuration](Sources.env, "MEMORY_CRON_INTERVAL", Constant(30 seconds)).value

  lazy val startServer: Boolean = Variables
    .firstValue[Boolean](
      BackgroundTasks.ServerStartName,
      Variables.maybeSource(Sources.env, "PROMETHEUS_SERVER"),
      Variables.maybeSource(Sources.env, BackgroundTasks.ServerStartName),
      Constant(false)
    )
    .value

  lazy val bindAddress: String = Variables
    .firstValue[String](
      BackgroundTasks.ServerAddressName,
      Variables.maybeSource(Sources.env, "MY_POD_IP"),
      Variables.maybeSource(Sources.env, BackgroundTasks.ServerAddressName),
      Variables.maybeSource(Sources.env, "PROMETHEUS_BIND_ADDRESS"),
      Constant("0.0.0.0") //
    )
    .value

  lazy val bindPort: Int = Variables
    .firstValue[Int](
      BackgroundTasks.ServerAddressPort,
      Variables.maybeSource(Sources.env, "PROMETHEUS_BIND_PORT"),
      Variables.maybeSource(Sources.env, BackgroundTasks.ServerAddressPort),
      Constant(8999) //
    )
    .value

  lazy val protoFormat: Boolean = Variables.source[Boolean](Sources.env, "PROMETHEUS_PROTOBUF", Constant(false)).value

  Sk8s.MemoryCron.startup(appInfo, memoryUpdate)

  if (startServer) {
    Try(new BackendServer(bindAddress, bindPort, protoFormat, BackgroundTasks.getConfig)) match {
      case Failure(th: Throwable) =>
        logger.error("", th)
        HealthStatus.unhealthy("prometheus-server")
      case Success(_) => // just ignore
    }
  }

  logger.info("Background tasks started")

}

object BackgroundTasks {
  val ServerStartName: String = "BACKEND_SERVER"
  val ServerAddressName       = "BACKEND_BIND_ADDRESS"
  val ServerAddressPort       = "BACKEND_BIND_PORT"

  def getConfig: String = {
    val buf = new StringBuilder("\n")
    Variables.dumpConfiguration({ in: String =>
      buf.append(in).append("\n")
    })
    buf.toString()
  }

}
