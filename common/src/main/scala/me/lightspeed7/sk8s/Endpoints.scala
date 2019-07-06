package io.timeli.sk8s

import java.time.LocalDateTime

import play.api.libs.json.{ Json, OFormat }

//
// Endpoints api domain
// ////////////////////////////
final case class Endpoints(kind: String, apiVersion: String, metadata: Metadata, subsets: Seq[Subset]) {

  //
  // Helpers
  // /////////////////////////
  def servicePort(portName: Option[String] = None): Int = {
    lazy val firstPort: Option[Int] = subsets.flatMap(_.ports.headOption).map(_.port).headOption
    lazy val filteredPort: Option[Int] = portName.flatMap(pn => subsets.flatMap(_.ports.filter(_.name.contains(pn)).map(_.port)).headOption)
    filteredPort orElse firstPort getOrElse 0
  }

  def findPortNameFor(port: Int): Option[String] = {
    val foo = subsets
      .find(_.ports.map(_.port).contains(port))
      .map { f =>
        f.ports.flatMap(_.name)
      }
      .getOrElse(Seq())

    foo.headOption
  }

  //
  // Lookups
  // //////////////////////////
  def hostIpsNoPort(port: Int): Seq[String] = {
    subsets
      .find(_.ports.map(_.port).contains(port))
      .map(_.addresses)
      .map { e => e.map(_.ip) }
      .getOrElse(Seq())
  }

  def hostIpsWithPort(port: Int): Seq[HostAndPort] = {
    subsets
      .find(_.ports.map(_.port).contains(port))
      .map(_.addresses)
      .map { e => e.map(_.ip) }
      .map { addrs => addrs.map(ip => HostAndPort(ip, port)) }
      .getOrElse(Seq())
  }

  def dnsHosts(port: Int): Set[String] = {
    subsets
      .find(_.ports.map(_.port).contains(port))
      .map(_.addresses)
      .map { endPts =>
        endPts
          //          .filterNot(_.targetRef.name.contains("arbiter"))
          .map { endP =>
            println(s"dnsHosts - $endP")
            endP.hostname orElse Option(endP.targetRef.name) match {
              case None => s"${metadata.name}.${metadata.namespace}.svc.cluster.local"
              case Some(host) =>
                s"$host.${metadata.name}.${metadata.namespace}.svc.cluster.local"
            }
          }
      }
      .getOrElse(Seq())
      .toSet
  }

  def hasHosts: Boolean = subsets.flatMap(_.addresses).nonEmpty

}

final case class Metadata(
    name: String,
    namespace: String,
    selfLink: String,
    uid: String,
    resourceVersion: String,
    creationTimestamp: LocalDateTime,
    labels: Map[String, String] = Map() //
)

final case class Subset(
    addresses: Seq[EndPoint],
    ports: Seq[Port] //
)

final case class Port(
    port: Int,
    protocol: String,
    name: Option[String] = None //
)

final case class EndPoint(
    ip: String,
    hostname: Option[String],
    nodeName: String,
    targetRef: TargetRef //
)

final case class TargetRef(
    kind: String,
    namespace: String,
    name: String,
    uid: String,
    resourceVersion: String //
)

object EndpointJson {
  implicit val _port: OFormat[Port] = Json.format[Port]
  implicit val _targetRef: OFormat[TargetRef] = Json.format[TargetRef]
  implicit val _endpoint: OFormat[EndPoint] = Json.format[EndPoint]
  implicit val _subset: OFormat[Subset] = Json.format[Subset]
  implicit val _metadata: OFormat[Metadata] = Json.format[Metadata]
  implicit val _endpoints: OFormat[Endpoints] = Json.format[Endpoints]
}

