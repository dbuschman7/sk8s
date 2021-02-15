package me.lightspeed7.sk8s

import com.typesafe.scalalogging.LazyLogging
import skuber.Container
import skuber.apps.v1beta1.Deployment

final case class KubernetesDeployment(skuber: Deployment) extends LazyLogging {

  def stats: Option[Deployment.Status] = skuber.status

  val namespace: String = skuber.metadata.namespace

  val name: String = skuber.name

  val fullName: String = s"$namespace.$name"

  lazy val available: Int = stats.map(_.availableReplicas).getOrElse(-1)

  lazy val desired: Int = stats.map(_.replicas).getOrElse(-1)

  lazy val container: Option[Container] = skuber.getPodSpec.flatMap(_.containers.find(_.name == skuber.name))

  lazy val image: Option[DockerImage] = container.flatMap(in => DockerImage.parse(in.image).toOption)

  def upgradeImageVersion(version: String): Option[Container] =
    image.flatMap { img =>
      container.map(_.copy(image = img.withVersion(version).toString))
    }

}
