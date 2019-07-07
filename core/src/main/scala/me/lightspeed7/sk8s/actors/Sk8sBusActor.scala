package me.lightspeed7.sk8s.actors

import akka.actor._
import com.typesafe.scalalogging.LazyLogging

trait Sk8sBusHelper {
  def publish(channel: SK8SBus.ChannelType, payload: Any): Unit = SK8SBus.publish(channel, payload)
}

abstract class Sk8sBusActor(channelName: String) extends Actor with Sk8sBusHelper with LazyLogging {

  protected lazy val actorClassName: String = this.getClass.getName

  // publish messages to the bus
  def publishToMyself(payload: Any): Unit = SK8SBus.publish(channelName, payload)

  // setup
  override def preStart() {
    logger.info(s"$actorClassName -> Actor Starting")
    super.preStart()
    SK8SBus.subscribe(self, channelName)
  }

  // teardown
  override def postStop() {
    SK8SBus.unsubscribe(self, channelName)
    logger.info(s"$actorClassName -> Actor Stopping")
    super.postStop()
  }

}
