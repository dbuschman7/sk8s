package me.lightspeed7.sk8s.actors

import akka.actor.{ Actor, Cancellable }
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

trait ScheduledCronActor extends Actor with LazyLogging {

  protected val ec: ExecutionContext = this.context.dispatcher

  protected def actorClassName: String

  private var cronTokenName: String    = "'Not Given'" // fix this in Scala 2.12 with trait parameter
  private var cancellable: Cancellable = _

  def registerCancelable(delay: FiniteDuration, interval: FiniteDuration, token: Any)(implicit ec: ExecutionContext): Unit = {
    cronTokenName = token.getClass.getName
    logger.info(s"$actorClassName Starting Scheduler -> Delay = ${delay.toString} Interval = ${interval.toString} with token of type $cronTokenName")
    cancellable = this.context.system.scheduler.schedule(delay, interval, self, token)
  }

  // setup
  override def preStart() {
    super.preStart()
  }

  // teardown
  override def postStop() {
    logger.info(s"$actorClassName Stop Scheduler -> Token of type $cronTokenName")
    if (cancellable != null) { cancellable.cancel }
    super.postStop()
  }

}
