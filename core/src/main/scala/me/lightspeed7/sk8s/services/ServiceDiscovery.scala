package me.lightspeed7.sk8s.services

import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.HostAndPort

class ServiceDiscovery(envMap: Map[String, String]) extends LazyLogging {

  private val lookupKey: String = "_SERVICE_PORT_SERVER"

  lazy val serviceNames: Seq[String] = {
    envMap
      .filter(_._1.contains(lookupKey))
      .keys
      .map { raw =>
        val idx = raw.indexOf(lookupKey)
        raw.substring(0, idx).toLowerCase
      }
  }.toSeq

  def getHostAndPort(serviceName: String): Option[HostAndPort] =
    serviceNames
      .find(_ == serviceName.toLowerCase)
      .flatMap { _ =>
        val host: Option[String] = envMap.get(s"${serviceName}_SERVICE_HOST".toUpperCase)
        val port: Option[Int]    = envMap.get(s"${serviceName}_SERVICE_PORT".toUpperCase).map(_.toInt)
        (host, port) match {
          case (Some(h), Some(p)) => Some(HostAndPort(h, p))
          case (_, _) =>
            logger.error(s"Sk8s - missing data for service '$serviceName'")
            None
        }
      }

}
