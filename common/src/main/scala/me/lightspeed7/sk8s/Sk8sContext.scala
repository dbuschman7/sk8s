package me.lightspeed7.sk8s

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

abstract class Sk8sContext(implicit val appInfo: AppInfo, val system: ActorSystem, val mat: Materializer) extends LazyLogging {

  lazy val internalSdkCalls: Boolean = Sk8s.serviceAccount().isKubernetes

  // akka implicit for types which use ActorMaterializer
  implicit val actMat: ActorMaterializer = mat match {
    case materializer: ActorMaterializer => materializer
    case _                               => throw new RuntimeException("Non-actor materializer found, have no idea to handle the config")
  }

  implicit val ec: ExecutionContext = system.dispatcher

  logger.info("Sk8sContext Stood Up")

}
