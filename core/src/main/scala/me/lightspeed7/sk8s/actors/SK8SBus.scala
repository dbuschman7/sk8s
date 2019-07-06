package me.lightspeed7.sk8s.actors

import akka.actor.{ActorRef, ActorSystem, Props, actorRef2Scala}
import akka.event.{EventBus, LookupClassification}
import akka.pattern.BackoffSupervisor
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.util.Try

object TimeliBus {

  def publish(channel: String, payload: Any): Unit = TimeliBus.publish(channel, payload)

  def subscribe(self: ActorRef, channel: String): Boolean = TimeliBus.subscribe(self, channel)

  def unsubscribe(self: ActorRef, channel: String): Boolean = TimeliBus.unsubscribe(self, channel)

  //
  // Message Bus
  // ////////////////////////////////
  private val TimeliBus = new LookupBusImpl

  private val log: Logger = LoggerFactory.getLogger(TimeliBus.getClass)

  private case class EventMessage(channel: String, payload: Any)

  type ChannelType = String

  private class LookupBusImpl extends EventBus with LookupClassification {
    type Event = EventMessage
    type Classifier = ChannelType
    type Subscriber = ActorRef

    override protected def classify(event: Event): Classifier = event.channel

    override protected def publish(event: Event, subscriber: Subscriber): Unit = {
      Try(subscriber ! event.payload)
        .recover {
          case ex => log.error(s"Unable to publish event on chaneel ${event.channel} to $subscriber", ex)
        }
    }

    override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.compareTo(b)

    override protected def mapSize: Int = 128

    def publish(channel: String, payload: Any): Unit = publish(EventMessage(channel, payload))
  }

  private lazy val BackoffMin = Variables.source[Duration](Sources.env, "DAEMON_BACKOFF_INITIAL_TIMEOUT", Constant(1 second), security = false) //
  private lazy val BackoffMax = Variables.source[Duration](Sources.env, "DAEMON_BACKOFF_MAX_TIMEOUT", Constant(60 seconds), security = false)

  def runDaemon(actorName: String, actorProps: Props)(implicit akka: ActorSystem): ActorRef = {
    val supervisor = BackoffSupervisor.props(
      actorProps,
      childName = actorName,
      minBackoff = BackoffMin.value.asInstanceOf[FiniteDuration],
      maxBackoff = BackoffMax.value.asInstanceOf[FiniteDuration],
      randomFactor = 0.2) // adds 20% "noise" to vary the intervals slightly

    akka.actorOf(supervisor, name = actorName + "-Supervisor")
  }

}
