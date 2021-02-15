package me.lightspeed7.sk8s.manifests

import me.lightspeed7.sk8s.manifests.Pod.TemplateSpec
import play.api.libs.json.{Json, OFormat}
import skuber.EnvVar

case class Pod(
  kind: String = "Pod",
  apiVersion: String = "v1",
  metadata: Common.Metadata,
  spec: TemplateSpec
) {

  import com.softwaremill.quicklens._

  def withImagePullSecrets(creds: String): Pod =
    this
      .modify(_.spec.imagePullSecrets)
      .using(in => in :+ Pod.TemplateSpec.Names(creds))

  def withListenPort(name: String, port: Int): Pod =
    this
      .modify(_.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.ports).using(in => in :+ Container.Port(port, name)))
      }

  def withLivenessProbe(probe: Probes.HttpProbe): Pod =
    this
      .modify(_.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.livenessProbe).setTo(Some(probe)))
      }

  def withReadinessProbe(probe: Probes.HttpProbe): Pod =
    this
      .modify(_.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.readinessProbe).setTo(Some(probe)))
      }

  def withEnvVar(envVar: EnvVar): Pod =
    this
      .modify(_.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.env).using(in => in :+ envVar))
      }

  def withEnvVars(envVars: List[EnvVar]): Pod =
    this
      .modify(_.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.env).using(in => in ++ envVars))
      }

  def withServiceAccount(name: Option[String]): Pod =
    name match {
      case None =>
        this
          .modify(_.spec.automountServiceAccountToken)
          .setTo(Some(false))
          .modify(_.spec.serviceAccountName)
          .setTo(None)
      case Some(acctName) =>
        this
          .modify(_.spec.automountServiceAccountToken)
          .setTo(None)
          .modify(_.spec.serviceAccountName)
          .setTo(Some(acctName))
    }

}

object Pod {

  def apply(name: String, namespace: String)(implicit cfg: Sk8sAppConfig): Pod = {

    val shortName = cfg.version.shortName(name)

    val container = Container(
      shortName,
      cfg.image.map(_.toString).getOrElse(""),
      cfg.imagePullPolicy,
      env = Sk8sAppConfig.defaultVars(cfg) ++ cfg.envVars,
      resources = cfg.request.toResource //
    )

    val pts: Pod.TemplateSpec = Pod.TemplateSpec(None, Seq(container))

    Pod(metadata = Common.Metadata(shortName, Some(namespace)), spec = pts)
  }

  case class Template(
    metadata: Common.Metadata,
    spec: TemplateSpec //
  )

  object Template {
    implicit val __json: OFormat[Template] = Json.format[Template]
  }

  case class TemplateSpec(
    affinity: Option[Common.Affinity] = None,
    containers: Seq[Container],
    serviceAccountName: Option[String] = None,
    automountServiceAccountToken: Option[Boolean] = None,
    restartPolicy: String = "Always",
    dnsPolicy: String = "ClusterFirst",
    imagePullSecrets: Seq[TemplateSpec.Names] = Seq(),
    volumes: Option[List[Volumes.Volume]] = None //
  )

  object TemplateSpec {
    implicit val __json: OFormat[TemplateSpec] = Json.format[TemplateSpec]

    case class Names(name: String)

    object Names {
      implicit val __json: OFormat[Names] = Json.format[Names]
    }

  }

  implicit val __json: OFormat[Pod] = Json.format[Pod]

}
