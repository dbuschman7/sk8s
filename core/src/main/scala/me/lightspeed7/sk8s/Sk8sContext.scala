package me.lightspeed7.sk8s

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

final case class Sk8sContext(appInfo: AppInfo)(implicit val system: ActorSystem, val mat: Materializer)
    extends AutoCloseable
    with LazyLogging {
  import scala.language.postfixOps

  lazy val internalSdkCalls: Boolean = Sk8s.serviceAccount().isKubernetes

  // akka implicit for types which use ActorMaterializer
  implicit val actMat: ActorMaterializer = mat match {
    case materializer: ActorMaterializer => materializer
    case _                               => throw new RuntimeException("Non-actor materializer found, have no idea to handle the config")
  }

  implicit val ec: ExecutionContext = system.dispatcher

  logger.info("Sk8sContext Stood Up")

  lazy val runMode: RunMode = RunMode.currentRunMode

  //
  // Closeable resource handling
  // ////////////////////////////////////
  private final case class Closeable(label: String, closeable: AutoCloseable)

  private var closeables: Seq[Closeable] = Seq.empty

  def registerCloseable[T <: AutoCloseable](label: String, closeable: => T): T =
    synchronized {
      logger.info(s"Register Closeable - $label")
      val temp = closeables :+ Closeable(label, closeable)
      closeables = temp
      closeable
    }

  def register[T](label: String, closeable: => T): T =
    synchronized {
      logger.info(s"Register Closeable - $label")

      val temp = closeables :+ Closeable(label,
                                         new AutoCloseable {
                                           override def close(): Unit = closeable
                                         }
        )

      closeables = temp
      closeable
    }

  def close(): Unit =
    synchronized {
      logger.info("Closing AutoCloseables ...")
      closeables.reverse // opposite of registration
        .foreach {
          case Closeable(l, c) =>
            logger.info(s"Closing - $l")
            c.close()
        }
    }

}

object Sk8sContext extends LazyLogging {
  import scala.language.postfixOps

  def create(appInfo: AppInfo): Sk8sContext = {

    val timeout: FiniteDuration = 10 seconds
    val actorSystemName         = appInfo.appName

    //
    // Create Internals
    // //////////////////////////////////
    val actorSystem: ActorSystem = {
      logger.info(s"Starting up ActorSystem '$actorSystemName' ...")
      ActorSystem(actorSystemName)
    }

    val decider: Supervision.Decider = { t: scala.Throwable =>
      logger.info("exception during graph, stopping" + t)
      t.printStackTrace()
      Supervision.Stop
    }

    implicit val materializer: ActorMaterializer = {
      logger.info("Starting up ActorMaterializer ...")
      ActorMaterializer(ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider))(actorSystem)
    }

    //
    val ctx = Sk8sContext(appInfo)(actorSystem, materializer)
    //

    //
    // Register Internals
    // /////////////////////////////////////
    ctx.registerCloseable(
      s"ActorSystem '$actorSystemName'",
      new AutoCloseable {
        override def close(): Unit = {
          logger.info(s"Shutting down ActorSystem '$actorSystemName' ...")
          Await.result(actorSystem.terminate(), timeout)
          ()
        }
      }
    )

    ctx.registerCloseable("ActorMaterializer",
                          new AutoCloseable {
                            override def close(): Unit = {
                              logger.info("Shutting down ActorMaterializer ...")
                              materializer.shutdown()
                            }
                          }
    )

    //
    ctx
  }
}
