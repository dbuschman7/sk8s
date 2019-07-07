package me.lightspeed7.sk8s.manifests

import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.{ Endpoints, Sk8s }

import scala.concurrent.{ ExecutionContext, Future }

final case class K8sServiceDefinition(k8sService: String, k8sNamespace: String) {
  def endPoints(implicit ec: ExecutionContext): Future[Option[Endpoints]] =
    Sk8s.serviceAccount().endpoints(k8sService, k8sNamespace)

  def k8sServiceDnsName(portStr: String): String = s"$k8sService.$k8sNamespace.svc.cluster.local$portStr"
}

abstract class DataService(val name: String, val port: Int, includePort: Boolean = true)(val k8sServices: List[K8sServiceDefinition])
    extends LazyLogging {

  def portStr: String = if (includePort) ":" + port.toString else ""

  private def toString(ep: Endpoints): Set[String] = ep.hostIpsWithPort(port).map(h => h.asString).toSet

  private def toStringNoPort(ep: Endpoints): Set[String] = ep.hostIpsNoPort(port).toSet

  def ipHosts(implicit executionContext: ExecutionContext): Future[(String, Set[String])] =
    lookup(if (includePort) toString else toStringNoPort)

  def dnsHosts(implicit executionContext: ExecutionContext): Future[(String, Set[String])] = {
    def toString(ep: Endpoints): Set[String] = ep.dnsHosts(port).map(h => h + portStr)
    lookup(toString)
  }

  private def lookup(func: Endpoints => Set[String])(implicit executionContext: ExecutionContext): Future[(String, Set[String])] = {

    def find(defs: List[K8sServiceDefinition]): Future[Option[Endpoints]] = {
      logger.debug("dnsHosts.find - " + defs)
      defs match {
        case Nil => Future successful None
        case head :: tail =>
          logger.debug("dnsHosts.head - " + head)
          head.endPoints.flatMap {
            case Some(endpts) => Future successful Some(endpts)
            case None         => find(tail)
          }
      }
    }

    find(k8sServices)
      .map { f =>
        logger.debug("dnsHosts.found - " + f.map(_.hostIpsWithPort(port)))
        val pair = f.map(ep => (s"${ep.metadata.name}.${ep.metadata.namespace}", func(ep)))
        logger.debug("dnsHosts:pair - " + pair)
        pair.getOrElse((k8sServices.headOption.map(_.k8sServiceDnsName(portStr)).getOrElse("unknown"), Set()))
      }
  }

  def allK8sServiceDnsName: List[String] = k8sServices.map(_.k8sServiceDnsName(portStr))

  def k8sServiceDnsName: String = k8sServices.headOption.map(_.k8sServiceDnsName(portStr)).getOrElse("unknown.dns.name")
}
