package me.lightspeed7.sk8s.backend

import akka.actor.Props
import ch.qos.logback.classic.LoggerContext
import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.actors.{ SK8SBus, Sk8sBusActor }
import me.lightspeed7.sk8s.{ AppInfo, RunMode, Sk8s, Sk8sContext }
import org.slf4j.LoggerFactory

final case class BackendApplication(info: AppInfo) extends LazyLogging with AutoCloseable {

  implicit val appInfo: AppInfo = info

  { // Do this first
    LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    logger.info(s"Application staring - $appInfo")
  }

  // Do this second
  implicit val appCtx: Sk8sContext = Sk8sContext.create(appInfo)

  logger.info("Starting background tasks ...")
  new BackgroundTasks()
  logger.info(s"Kubernetes - ${Sk8s.isKubernetes()}")
  scala.sys.addShutdownHook(shutdown())

  //
  // return code handling
  // //////////////////////
  private var returnCode: Int = -1

  def shutdown(): Unit = returnCode = 0

  def shutdown(ex: Throwable): Unit = {
    logger.error("Abnormal termination", ex)
    returnCode = 1
  }

  def runUntilStopped(): Unit = {
    logger.info("Activating Daemon Mode ...")
    appCtx.system.actorOf(Props(new ShutdownActor())) // start up the shutdown actor
    do {
      Thread.sleep(1000)
    } while (returnCode < 0)

    if (RunMode.currentRunMode.useSystemExit) System.exit(returnCode)
  }

  class ShutdownActor extends Sk8sBusActor(BackendApplication.channel) {
    def receive: Receive = {
      case None =>
        logger.info("Shutdown received with code - 0")
        returnCode = 0
      case Some(code: Int) =>
        logger.info("Shutdown received with code - " + code)
        returnCode = code
      case unknown =>
        logger.warn("Unknown shutdown message received - " + unknown)
    }
  }

  override def close(): Unit = {
    appCtx.close()
    shutdown()
  }
}

object BackendApplication {
  val channel = "SHUTDOWN"

  def shutdown: Unit /*                 */ = SK8SBus.publish(channel, None)

  def terminate(returnCode: Int = 1): Unit = SK8SBus.publish(channel, Some(returnCode))
}
