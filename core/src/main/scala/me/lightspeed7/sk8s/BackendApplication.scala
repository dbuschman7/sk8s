package me.lightspeed7.sk8s

import akka.actor.{ Actor, ActorSystem, Props }
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings, Supervision }
import ch.qos.logback.classic.LoggerContext
import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.actors.{ SK8SBus, Sk8sBusActor }
import me.lightspeed7.sk8s.util.Closeables
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.{ FiniteDuration, _ }

final case class BackendApplication(info: AppInfo, ctx: Sk8sContext) extends LazyLogging with AutoCloseable {

  implicit val appInfo: AppInfo = info

  { // Do this first
    LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    logger.info(s"Application staring - $appInfo")
  }

  implicit val actorSystem: ActorSystem = {

    val timeout: FiniteDuration = 10 seconds
    val actorSystemName         = appInfo.appName

    logger.info(s"Starting up ActorSystem '$actorSystemName' ...")
    val ac = ActorSystem(actorSystemName)
    Closeables.registerCloseable(
      s"ActorSystem '$actorSystemName'",
      new AutoCloseable {
        override def close(): Unit = {
          logger.info(s"Shutting down ActorSystem '$actorSystemName' ...")
          Await.result(ac.terminate(), timeout)
          ()
        }
      }
    )
    ac
  }

  private val decider: Supervision.Decider = { t =>
    logger.info("exception during graph, stopping" + t)
    t.printStackTrace()
    Supervision.Stop
  }

  implicit val materializer: ActorMaterializer = {
    logger.info("Starting up ActorMaterializer ...")
    val mat = ActorMaterializer(ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider))

    Closeables.registerCloseable("ActorMaterializer", new AutoCloseable {
      override def close(): Unit = {
        logger.info("Shutting down ActorMaterializer ...")
        mat.shutdown()
      }
    })

    mat
  }

  implicit val appCtx: Sk8sContext = ctx

  protected val backgroundTasks = new BackgroundTasks()(appCtx)

  logger.info(s"Kubernetes - ${Sk8s.isKubernetes()}")

  private val _: AppRoutesServer = new AppRoutesServer(this, ServerConfig())(appCtx)

  private var returnCode: Int = -1

  def shutdown(): Unit = returnCode = 0

  def shutdown(ex: Throwable): Unit = {
    logger.error("Abnormal termination", ex)
    returnCode = 1
  }

  def runUntilStopped(): Unit = {
    logger.info("Activating Daemon Mode ...")
    actorSystem.actorOf(Props(new ShutdownActor())) // start up the shutdown actor
    do {
      Thread.sleep(1000)
    } while (returnCode < 0)

    if (!RunMode.Test.isCurrent && !RunMode.FuncTest.isCurrent) System.exit(returnCode)
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

  scala.sys.addShutdownHook(shutdown())

  override def close(): Unit = {
    Closeables.close()
    shutdown()
  }
}

object BackendApplication {
  val channel = "SHUTDOWN"

  def shutdown: Unit /*                 */ = SK8SBus.publish(channel, None)

  def terminate(returnCode: Int = 1): Unit = SK8SBus.publish(channel, Some(returnCode))
}
