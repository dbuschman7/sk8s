package me.lightspeed7.sk8s.manifests

import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

import me.lightspeed7.sk8s.util.AlphaId
import play.api.libs.json.{ Json, OFormat }
import skuber.EnvVar

import scala.concurrent.duration.{ Duration, FiniteDuration }

//
// Single Job
// ///////////////////////////////////
case class Job(
    kind: String = "Job",
    apiVersion: String = "batch/v1",
    metadata: Common.Metadata,
    spec: Job.Spec
) {

  import com.softwaremill.quicklens._

  def withLivenessProbe(probe: Probes.HttpProbe): Job =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.livenessProbe).setTo(Some(probe)))
      }

  def withReadinessProbe(probe: Probes.HttpProbe): Job =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.readinessProbe).setTo(Some(probe)))
      }

  def withEnvVars(envVars: List[EnvVar]): Job =
    this
      .modify(_.spec.template.spec.containers)
      .using { cnts: Seq[Container] =>
        Seq(cnts.head.modify(_.env).using(in => in ++ envVars))
      }

  def withImagePullSecrets(creds: String): Job =
    this
      .modify(_.spec.template.spec.imagePullSecrets)
      .using(in => in :+ Pod.TemplateSpec.Names(creds))

  def withServiceAccount(name: Option[String]): Job = name match {
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

  def withSecretMount(secretName: String, volumeName: String, mountDir: String, defaultMode: Int = 420): Job = {

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

}

object Job {

  implicit val __json: OFormat[Job] = Json.format[Job]

  case class Spec(
      completions: Int = 1, // number of times to run
      parallelism: Int = 1, // number of pods that can run in parallel
      backoffLimit: Int = 1, // number of retries before throwing error
      activeDeadlineSeconds: Int = 300, // time to allow job to run
      template: Pod.Template
  )

  object Spec {
    implicit val __json: OFormat[Spec] = Json.format[Spec]
  }

  def apply(name: String, namespace: String)(implicit cfg: Sk8sAppConfig): Job = {

    val container = Container(
      name,
      cfg.image.map(_.toString).getOrElse(""),
      cfg.imagePullPolicy,
      env = cfg.defaultVars,
      resources = cfg.request.toResource //
    )

    val pts: Pod.TemplateSpec = Pod.TemplateSpec(None, Seq(container))
    val pt: Pod.Template =
      Pod.Template(Common.Metadata(name, labels = Some(Map("app" -> name, "metrics" -> "prometheus"))), pts)
    Job(metadata = Common.Metadata(name, Some(namespace)), spec = Job.Spec(template = pt))
  }

}

final case class JobReference(jobPrefix: String, jobId: AlphaId) {
  def jobName: String = s"$jobPrefix-${jobId.id}"
}

object JobReference {
  implicit val __json: OFormat[JobReference] = Json.format[JobReference]
}

case class JobStatus(internal: skuber.batch.Job.Status) {

  private def translate(status: Option[Int], default: Boolean): Boolean = status.map(_ != 0).getOrElse(default)

  def isComplete: Boolean = internal.completionTime.isDefined

  def isError: Boolean = isComplete && translate(internal.failed, default = false)

  def isSuccess: Boolean = isComplete && translate(internal.succeeded, default = false)

  def duration: FiniteDuration =
    (internal.startTime, internal.completionTime) match {
      case (Some(start), Some(comp)) =>
        val compMillis: Long  = comp.toLocalDateTime.toInstant(ZoneOffset.UTC).toEpochMilli
        val startMillis: Long = start.toLocalDateTime.toInstant(ZoneOffset.UTC).toEpochMilli
        FiniteDuration(compMillis - startMillis, TimeUnit.MILLISECONDS)
      case (_, _) =>
        Duration.Zero
    }
}
