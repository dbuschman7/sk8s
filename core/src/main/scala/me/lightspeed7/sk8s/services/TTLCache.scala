package me.lightspeed7.sk8s.services

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class CacheConfig(cacheTtl: FiniteDuration = 25 hours, reaper: FiniteDuration = 1 hour)

object CacheConfig {
  def defaults: CacheConfig = CacheConfig()
}

object TTLCache {

  def apply[K, V](config: CacheConfig)(implicit akka: ActorSystem): TTLCache[K, V] =
    apply(config.cacheTtl, config.reaper)

  def apply[K, V](cacheTtl: FiniteDuration, reaper: FiniteDuration)(implicit akka: ActorSystem): TTLCache[K, V] = {
    implicit val ec: ExecutionContext = akka.dispatcher

    val cache = new TTLCache[K, V](cacheTtl.toMillis)
    akka.scheduler.schedule(reaper, reaper)(cache.removeExpired())
    cache
  }

}

final case class TTLCache[K, V](ttl: Long) extends LazyLogging {

  private def clock(): Long = System.currentTimeMillis

  class ValueWrapper[T](val value: T, var insertTime: Long = clock()) {
    def expired: Boolean = insertTime + ttl < clock()
  }

  private val cache: TrieMap[K, ValueWrapper[V]] = TrieMap.empty[K, ValueWrapper[V]]

  def get(k: K): Option[V] = cache.get(k).filterNot(_.expired).map(_.value)

  def getOrElseUpdate(k: K, f: K => V): Option[V] = {
    val value: ValueWrapper[V] = cache.getOrElseUpdate(k, new ValueWrapper(f(k)))
    if (value.expired) None else Some(value.value)
  }

  // Resets the clock for any accessed keys, possible stale value replacement
  def getWithReset(k: K): Option[V] =
    cache
      .get(k)
      .filterNot(_.expired)
      .map { v =>
        v.insertTime = clock()
        v.value
      }

  def contains(k: K): Boolean = cache.contains(k)

  def put(key: K, value: V): TTLCache[K, V] = {
    cache.put(key, new ValueWrapper(value))
    this
  }

  def evict(k: K): TTLCache[K, V] = {
    cache.remove(k)
    this
  }

  def clear: TTLCache[K, V] = {
    cache.clear
    this
  }

  def size: Int = cache.size

  def keys: collection.Set[K] = cache.keySet

  def removeExpired(): Unit = {
    logger.trace("TTLCache Reaper starting ....")
    val keysToRemove = cache.filter {
      case (_, v) => v.expired
    }.keys

    keysToRemove.map { key =>
      logger.trace(s"TTLCache Reaper removing - '$key' ")
      cache.remove(key)
    }

    logger.trace("TTLCache Reaper completed")
  }
}
