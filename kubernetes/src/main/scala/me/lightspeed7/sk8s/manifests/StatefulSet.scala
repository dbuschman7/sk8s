package me.lightspeed7.sk8s.manifests

import play.api.libs.json.{ Json, OFormat }
import skuber.Pod.Affinity.{ PodAffinityTerm, PodAntiAffinity }
import skuber.{ ConfigMap, EnvVar }

//
// Stateful Set
// ///////////////////////////////////
case class StatefulSet(
    kind: String = "StatefulSet",
    apiVersion: String = "apps/v1",
    metadata: Common.Metadata,
    spec: StatefulSet.Spec //
) {

  import com.softwaremill.quicklens._

  def withListenPort(name: String, port: Int): StatefulSet =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.ports).using(in => in :+ Container.Port(port, name)))
      }

  def withLivenessProbe(probe: Probes.HttpProbe): StatefulSet =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.livenessProbe).setTo(Some(probe)))
      }

  def withReadinessProbe(probe: Probes.HttpProbe): StatefulSet =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.readinessProbe).setTo(Some(probe)))
      }

  def withEnvVars(envVars: List[EnvVar]): StatefulSet =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.env).using(in => in ++ envVars))
      }

  def withImagePullSecrets(creds: String): StatefulSet =
    this
      .modify(_.spec.template.spec.imagePullSecrets)
      .using(in => in :+ Pod.TemplateSpec.Names(creds))

  def withAntiAffinity(implicit cfg: Sk8sAppConfig): StatefulSet = {
    val anti: PodAntiAffinity =
      PodAntiAffinity.apply(List(PodAffinityTerm(spec.selector.map(_.toLabelSelector(this)), topologyKey = "kubernetes.io/hostname")))
    this
      .modify(_.spec.template.spec.affinity)
      .setTo(Option(Common.Affinity(podAntiAffinity = Some(anti))))
  }

  def withConfigMap(configMap: ConfigMap, volumeName: String, mountDir: String, defaultMode: Int = 420): StatefulSet = {

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

  def withSecretMount(secretName: String, volumeName: String, mountDir: String, defaultMode: Int = 420): StatefulSet = {

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

  def withServiceAccount(name: Option[String]): StatefulSet = name match {
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

object StatefulSet {

  case class Spec(
      replicas: Int = 1,
      serviceName: String,
      selector: Option[Common.Selector] = None,
      template: Pod.Template //
  )

  def apply(name: String, namespace: String)(implicit cfg: Sk8sAppConfig): StatefulSet = {

    val shortName = cfg.version.shortName(name)

    val container = Container(
      shortName,
      cfg.image.map(_.toString).getOrElse(""),
      cfg.imagePullPolicy,
      env = Sk8sAppConfig.defaultVars(cfg) ++ cfg.envVars,
      resources = cfg.request.toResource //
    )

    val pts: Pod.TemplateSpec = Pod.TemplateSpec(None, Seq(container))
    val pt: Pod.Template =
      Pod.Template(Common.Metadata(shortName, labels = Some(Map("app" -> shortName, "metrics" -> "prometheus"))), pts)
    val spec: Spec =
      StatefulSet.Spec(cfg.replicas, shortName + "-svc", Some(Common.Selector(Some(Map("app" -> shortName)))), pt)

    StatefulSet(metadata = Common.Metadata(shortName, Some(namespace)), spec = spec)
  }

  object Spec {
    implicit val __json: OFormat[Spec] = Json.format[Spec]
  }

  implicit val __json: OFormat[StatefulSet] = Json.format[StatefulSet]
}
