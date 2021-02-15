package me.lightspeed7.sk8s.util

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object AsyncRunner extends LazyLogging {

  def runFor[T](runTime: FiniteDuration, refreshInterval: FiniteDuration = 2 seconds)(
    block: => Unit
  )(implicit akka: ActorSystem): Unit = {
    implicit val ec: ExecutionContext = akka.dispatcher

    val stopTime: Long = System.currentTimeMillis() + runTime.toMillis

    def schedule(runnable: Runnable): Unit = {
      logger.debug("Scheduling runnable ...")
      akka.scheduler.scheduleOnce(refreshInterval)(runnable.run())
      ()
    }

    val runnable = new Runnable {
      override def run(): Unit = {
        logger.debug("Runnable running")

        block // call-by-name

        if (System.currentTimeMillis() < stopTime) {
          schedule(this)
        } else {
          logger.debug("Runnable not scheduled")
        }
      }
    }

    schedule(runnable)
  }

}
