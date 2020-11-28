package me.lightspeed7.sk8s.actors

import akka.actor.{ actorRef2Scala, ActorRef, ActorSystem, Props }
import akka.event.{ EventBus, LookupClassification }
import akka.pattern.{ BackoffOpts, BackoffSupervisor }
import me.lightspeed7.sk8s.actors.SK8SBus.BackoffMax
import me.lightspeed7.sk8s.{ Constant, Sources, Variables }
import org.slf4j.{ Logger, LoggerFactory }

import scala.concurrent.duration._
import scala.util.Try

object SK8SBus {

  def publish(channel: String, payload: Any): Unit = TheBus.publish(channel, payload)

  def subscribe(self: ActorRef, channel: String): Boolean = TheBus.subscribe(self, channel)

  def unsubscribe(self: ActorRef, channel: String): Boolean = TheBus.unsubscribe(self, channel)

  //
  // Message Bus
  // ////////////////////////////////
  private val TheBus = new LookupBusImpl

  private val log: Logger = LoggerFactory.getLogger(TheBus.getClass)

  private case class EventMessage(channel: String, payload: Any)

  type ChannelType = String

  private class LookupBusImpl extends EventBus with LookupClassification {
    type Event      = EventMessage
    type Classifier = ChannelType
    type Subscriber = ActorRef

    override protected def classify(event: Event): Classifier = event.channel

    override protected def publish(event: Event, subscriber: Subscriber): Unit =
      Try(subscriber ! event.payload)
        .recover {
          case ex => log.error(s"Unable to publish event on channel ${event.channel} to $subscriber", ex)
        }

    override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.compareTo(b)

    override protected def mapSize: Int = 128

    def publish(channel: String, payload: Any): Unit = publish(EventMessage(channel, payload))
  }

  private lazy val BackoffMin =
    Variables.source[Duration](Sources.env, "DAEMON_BACKOFF_INITIAL_TIMEOUT", Constant(1 second), security = false) //
  private lazy val BackoffMax =
    Variables.source[Duration](Sources.env, "DAEMON_BACKOFF_MAX_TIMEOUT", Constant(60 seconds), security = false)

  def runDaemon(actorName: String, actorProps: Props)(implicit akka: ActorSystem): ActorRef = {

    val opts = BackoffOpts.onFailure(
      actorProps,
      actorName,
      BackoffMin.value.asInstanceOf[FiniteDuration],
      BackoffMax.value.asInstanceOf[FiniteDuration],
      randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
    )

    akka.actorOf(BackoffSupervisor.props(opts), name = actorName + "-Supervisor")
  }

}
