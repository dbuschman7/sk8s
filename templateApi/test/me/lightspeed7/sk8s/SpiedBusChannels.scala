package me.lightspeed7.sk8s

import me.lightspeed7.sk8s.actors.Sk8sBusActor

import scala.collection.mutable

class SpiedChannelState[T] {
  val channelData = new mutable.ArrayBuffer[T]()

  def size: Int = channelData.size

  def get: Seq[T] = channelData.toList // making a copy here

  def clear(): Unit = channelData.clear()
}

class SpiedChannelActor[T](channel: String, state: SpiedChannelState[T]) extends Sk8sBusActor(channel) {
  override def receive: Receive = {
    case event: T => state.channelData.append(event)
  }
}
