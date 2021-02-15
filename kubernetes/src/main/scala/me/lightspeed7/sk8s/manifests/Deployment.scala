package me.lightspeed7.sk8s.manifests

import play.api.libs.json.{Json, OFormat}
import skuber.Pod.Affinity.{PodAffinityTerm, PodAntiAffinity}
import skuber.{ConfigMap, EnvVar, LabelSelector}

case class Deployment(
  kind: String = "Deployment",
  apiVersion: String = "apps/v1",
  metadata: Common.Metadata,
  spec: Deployment.Spec //
) {

  import com.softwaremill.quicklens._

  def withListenPort(name: String, port: Int): Deployment =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.ports).using(in => in :+ Container.Port(port, name)))
      }

  def withLivenessProbe(probe: Probes.HttpProbe): Deployment =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.livenessProbe).setTo(Some(probe)))
      }

  def withReadinessProbe(probe: Probes.HttpProbe): Deployment =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.readinessProbe).setTo(Some(probe)))
      }

  def withEnvVar(envVar: EnvVar): Deployment =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.env).using(in => in :+ envVar))
      }

  def withEnvVars(envVars: List[EnvVar]): Deployment =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.env).using(in => in ++ envVars))
      }

  def withImagePullSecrets(creds: String): Deployment =
    this
      .modify(_.spec.template.spec.imagePullSecrets)
      .using(in => in :+ Pod.TemplateSpec.Names(creds))

  def withAntiAffinity(implicit cfg: Sk8sAppConfig): Deployment = {
    import skuber.LabelSelector.dsl._
    val selector: LabelSelector.Requirement = "app" is this.metadata.name
    val anti: PodAntiAffinity =
      PodAntiAffinity.apply(List(PodAffinityTerm(Some(selector), topologyKey = "kubernetes.io/hostname")))
    this
      .modify(_.spec.template.spec.affinity)
      .setTo(Option(Common.Affinity(podAntiAffinity = Some(anti))))
  }

  def withConfigMap(configMap: ConfigMap, volumeName: String, mountDir: String, defaultMode: Int = 420): Deployment = {

    val vol = Volumes.makeConfigMapVolume(volumeName, configMap.name, defaultMode)
    val mnt = Volumes.VolumeMount(vol.name, mountDir)

    this
      // add vol
      .modify(_.spec.template.spec.volumes)
      .using { in =>
        Option(in.getOrElse(List()) :+ vol)
      }
      // add mnt
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.volumeMounts).using(in => Option(in.getOrElse(List()) :+ mnt)))
      }
  }

  def withSecretMount(secretName: String, volumeName: String, mountDir: String, defaultMode: Int = 420): Deployment = {

    val vol = Volumes.makeSecretVolume(volumeName, secretName, defaultMode)
    val mnt = Volumes.VolumeMount(vol.name, mountDir)

    this
      // add vol
      .modify(_.spec.template.spec.volumes)
      .using { in =>
        Option(in.getOrElse(List()) :+ vol)
      }
      // add mnt
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.volumeMounts).using(in => Option(in.getOrElse(List()) :+ mnt)))
      }
  }

  def withServiceAccount(name: Option[String]): Deployment =
    name match {
      case None =>
        this
          .modify(_.spec.template.spec.automountServiceAccountToken)
          .setTo(Some(false))
          .modify(_.spec.template.spec.serviceAccountName)
          .setTo(None)
      case Some(acctName) =>
        this
          .modify(_.spec.template.spec.automountServiceAccountToken)
          .setTo(None)
          .modify(_.spec.template.spec.serviceAccountName)
          .setTo(Some(acctName))
    }

}

object Deployment {

  implicit val __json: OFormat[Deployment] = Json.format[Deployment]

  case class Spec(
    replicas: Int = 1,
    selector: Option[Common.Selector] = None,
    template: Template //
  )

  object Spec {
    implicit val __json: OFormat[Spec] = Json.format[Spec]
  }

  case class Template(
    metadata: Common.Metadata,
    spec: Pod.TemplateSpec //
  )

  object Template {
    implicit val __json: OFormat[Template] = Json.format[Template]
  }

  def apply(name: String, namespace: String)(implicit cfg: Sk8sAppConfig): Deployment = {

    val container = Container(
      cfg.name,
      cfg.image.map(_.toString).getOrElse(""),
      cfg.imagePullPolicy,
      env = Sk8sAppConfig.defaultVars(cfg) ++ cfg.envVars,
      resources = cfg.request.toResource //
    )

    val pts: Pod.TemplateSpec = Pod.TemplateSpec(None, Seq(container))
    val pt: Template =
      Template(Common.Metadata(cfg.name, labels = Some(Map("app" -> cfg.name, "metrics" -> "prometheus"))), pts)
    val spec: Spec = Deployment.Spec(cfg.replicas, Some(Common.Selector(Some(Map("app" -> cfg.name)))), pt)

    Deployment(metadata = Common.Metadata(name, Some(namespace)), spec = spec)
  }

}
