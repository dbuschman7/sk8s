package me.lightspeed7.sk8s

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.typesafe.scalalogging.{LazyLogging, StrictLogging}
import play.api.libs.json.Json
import skuber._
import skuber.apps.v1beta1._

import scala.concurrent.Future

class Kubernetes(implicit val appCtx: Sk8sContext) extends LazyLogging {

  import appCtx._

  lazy val k8s: _root_.skuber.api.client.RequestContext = k8sInit

  def getDeployments(namespace: String): Future[Set[KubernetesDeployment]] =
    k8s.listInNamespace[DeploymentList](namespace).map { raw =>
      try {
        val pp = Json.prettyPrint(Json.toJson(raw))
        Files.write(Paths.get(s"/opt/docker/$namespace.json"), pp.getBytes(StandardCharsets.UTF_8))
      } catch {
        case ex: Exception =>
          logger.error("Unable to save deps to file", ex)
      }

      raw.map { dep: Deployment =>
        logger.info(s"Raw deployment found - ${dep.metadata.namespace}.${dep.metadata.name}")
        KubernetesDeployment(dep)
      }.toSet
    }

  def getNodes: Future[Seq[Node]] = {
    import skuber.json.format._
    k8s
      .list[NodeList]()
      .map { raw =>
        try {
          val pp = Json.prettyPrint(Json.toJson(raw))
          Files.write(Paths.get("/opt/docker/nodes.json"), pp.getBytes(StandardCharsets.UTF_8))
        } catch {
          case ex: Exception =>
            logger.error("Unable to save nodes to file", ex)
        }

        raw
      }
  }

  def getPods: Future[Seq[Pod]] = {
    import skuber.json.format._
    println("getPods")
    k8s
      .list[PodList]()
      .map { raw =>
        println(s"getPods - $raw")
        try {
          val pp = Json.prettyPrint(Json.toJson(raw))
          Files.write(Paths.get("/opt/docker/pods.json"), pp.getBytes(StandardCharsets.UTF_8))
        } catch {
          case ex: Exception =>
            logger.error("Unable to save nodes to file", ex)
        }

        raw
      }
  }

  def getAllDeployments(ns: String = "default"): Future[Set[KubernetesDeployment]] = {
    val futures = {
      logger.info(s"Fetching deployments in namespace $ns")
      getDeployments(ns).map { deps =>
        logger.info(s"Found    deployments in namespace $ns : ${deps.size}")
        deps
      }
    }

    futures.map { deps: Set[KubernetesDeployment] =>
      logger.info(s"Total deployments - ${deps.size}")
      deps.foreach(d => println(s"getAllDeployments($ns) - ${d.fullName}"))
      deps
    }
  }

  def getDeployment(deployment: String, ns: String = "default"): Future[Option[KubernetesDeployment]] =
    getAllDeployments(ns)
      .map { deps =>
        deps.find(d => d.skuber.name.contains(deployment))
      }

  def getConfigMaps(namespace: String): Future[Seq[ConfigMap]] = {
    import skuber.json.format._
    // akka.http.client.parsing.max-content-length
    k8s.listInNamespace[ConfigMapList](namespace).map(_.items)
  }

  def getConfigMap(namespace: String, mapName: String): Future[ConfigMap] = {
    import skuber.json.format._
    k8s.getInNamespace[ConfigMap](mapName, namespace)
  }

}
