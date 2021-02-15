package me.lightspeed7.sk8s.manifests

import play.api.libs.json
import play.api.libs.json._
import skuber.Pod.Affinity.{PodAffinity, PodAntiAffinity}
import skuber.{EnvVar, LabelSelector, Resource}

//
// Core classes
// //////////////////////////
object Common {

  final case class Version(major: Int = 1, minor: Int = 0, patch: Int = 0) {
    def fullName(name: String): String = s"$name-$major-$minor-$patch"

    def shortName(name: String): String = s"$name-$major-$minor"
  }

  final case class Requests(cpu: Double = 0.5, memoryMb: Int = 1024) {

    def toResource: Option[Resource.Requirements] = {

      var resources: Option[Resource.Requirements] = None

      //
      // Borrowed from Skuber
      // /////////////////////////////
      def limitCPU(cpu: Resource.Quantity): Unit = addResourceLimit(Resource.cpu, cpu)

      def limitMemory(mem: Resource.Quantity): Unit = addResourceLimit(Resource.memory, mem)

      def addResourceLimit(name: String, limit: Resource.Quantity): Unit = {
        val currResources = resources.getOrElse(Resource.Requirements())
        val newLimits     = currResources.limits + (name -> limit)
        resources = Some(Resource.Requirements(newLimits, currResources.requests))
      }

      def requestCPU(cpu: Resource.Quantity): Unit = addResourceRequest(Resource.cpu, cpu)

      def requestMemory(mem: Resource.Quantity): Unit = addResourceRequest(Resource.memory, mem)

      def addResourceRequest(name: String, req: Resource.Quantity): Unit = {
        val currResources = resources.getOrElse(Resource.Requirements())
        val newReqs       = currResources.requests + (name -> req)
        resources = Some(Resource.Requirements(currResources.limits, newReqs))
      }

      requestCPU(Resource.Quantity(cpu.toString))
      limitCPU(Resource.Quantity("3"))
      requestMemory(Resource.Quantity(s"${memoryMb}m"))

      resources
    }

  }

  sealed trait Java

  case object Java8 extends Java

  case object Java11 extends Java

  object Java {

    implicit val _reads: Reads[Java] = new Reads[Java] {

      def reads(json: JsValue): JsResult[Java] =
        json match {
          case JsString(s) =>
            s match {
              case "java8"  => JsSuccess(Java8)
              case "java11" => JsSuccess(Java11)
            }
          case _ => JsError("error.expected.java.version")
        }
    }

    implicit val _writes: Writes[Java] = json.Writes {
      case Java8  => JsString("java8")
      case Java11 => JsString("java11")
    }

    implicit val _json: Format[Java] = Format(_reads, _writes)

  }

  case class Metadata(
    name: String,
    namespace: Option[String] = None,
    labels: Option[Map[String, String]] = None,
    annotations: Option[Map[String, String]] = None //
  )

  object Metadata {
    implicit val __json: OFormat[Metadata] = Json.format[Metadata]
  }

  case class Selector(matchLabels: Option[Map[String, String]] = None) {

    def toLabelSelector(statefulSet: StatefulSet): LabelSelector = {
      import skuber.LabelSelector.dsl._
      val name: String =
        statefulSet.spec.template.metadata.labels.flatMap(_.get("app")).getOrElse(statefulSet.metadata.name)
      "app" is name
    }

    def toLabelSelector(job: Job): LabelSelector = {
      import skuber.LabelSelector.dsl._
      val name: String = job.spec.template.metadata.labels.flatMap(_.get("app")).getOrElse(job.metadata.name)
      "app" is name
    }

    def toLabelSelector(deploy: Deployment): LabelSelector = {
      import skuber.LabelSelector.dsl._
      val name: String = deploy.spec.template.metadata.labels.flatMap(_.get("app")).getOrElse(deploy.metadata.name)
      "app" is name
    }
  }

  object Selector {
    implicit val __json: OFormat[Selector] = Json.format[Selector]
  }

  case class Affinity(podAffinity: Option[PodAffinity] = None, podAntiAffinity: Option[PodAntiAffinity] = None)

  object Affinity {
    import skuber.json._
    import skuber.json.format._

    implicit val __json: OFormat[Affinity] = Json.format[Affinity]
  }

}

object Volumes {

  sealed trait VolumeRef

  case class Secret(secretName: String, defaultMode: Int = 420) extends VolumeRef

  object Secret {
    implicit val __json: OFormat[Secret] = Json.format[Secret]
  }

  case class ConfigMap(name: String, defaultMode: Int = 420) extends VolumeRef

  object ConfigMap {
    implicit val __json: OFormat[ConfigMap] = Json.format[ConfigMap]
  }

  case class Volume(name: String, configMap: Option[ConfigMap] = None, secret: Option[Secret] = None)

  object Volume {

    val __volumeRefJsonWrites: Writes[VolumeRef] = Writes[VolumeRef] {
      case cm: ConfigMap => ConfigMap.__json.writes(cm)
      case s: Secret     => Secret.__json.writes(s)
      case unkown        => throw new IllegalArgumentException(s"Unknown VolumeRef - $unkown")
    }

    val __volumeRefJsonReads: Reads[VolumeRef] = new Reads[VolumeRef] {

      def reads(json: JsValue): JsResult[VolumeRef] = {
        val obj = json.as[JsObject]
        obj.fields.find(_._1 == "secretName").map(_._2) match {
          case None    => ConfigMap.__json.reads(obj)
          case Some(_) => Secret.__json.reads(obj)
        }
      }
    }

    implicit val __volumeRefJson: Format[VolumeRef] = Format(__volumeRefJsonReads, __volumeRefJsonWrites)
    implicit val __json: Format[Volume]             = Json.format[Volume]
  }

  case class VolumeMount(name: String, mountPath: String)

  object VolumeMount {
    implicit val __json: OFormat[VolumeMount] = Json.format[VolumeMount]
  }

  def makeConfigMapVolume(volName: String, cmName: String, defaultMode: Int = 420) =
    Volume(volName, configMap = Some(ConfigMap(cmName, defaultMode)))

  def makeSecretVolume(volName: String, secretName: String, defaultMode: Int = 420) =
    Volume(volName, secret = Some(Secret(secretName, defaultMode)))

}

//
// Common
// ///////////////////
case class Container(
  name: String,
  image: String,
  imagePullPolicy: String,
  ports: Seq[Container.Port] = Seq(),
  env: List[EnvVar] = List(),
  resources: Option[Resource.Requirements],
  livenessProbe: Option[Probes.HttpProbe] = None,
  readinessProbe: Option[Probes.HttpProbe] = None,
  volumeMounts: Option[List[Volumes.VolumeMount]] = None //
)

object Container {

  case class Port(containerPort: Int, name: String, protocol: String = "TCP")

  object Port {
    implicit val __json: OFormat[Port] = Json.format[Port]
  }

  import skuber.json.format._

  implicit val __json: OFormat[Container] = Json.format[Container]
}

object Probes {

  case class HTTPGet(
    port: Int,
    path: String,
    scheme: String //
  )

  object HTTPGet {
    implicit val __json: OFormat[HTTPGet] = Json.format[HTTPGet]
  }

  case class HttpProbe(
    httpGet: HTTPGet,
    initialDelaySeconds: Int,
    timeoutSeconds: Int,
    periodSeconds: Int,
    successThreshold: Int,
    failureThreshold: Int //
  )

  object HttpProbe {
    implicit val __json: OFormat[HttpProbe] = Json.format[HttpProbe]
  }
}
