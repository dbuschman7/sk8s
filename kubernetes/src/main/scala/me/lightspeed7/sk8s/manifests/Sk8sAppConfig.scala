package me.lightspeed7.sk8s.manifests

import java.util.Base64

import me.lightspeed7.sk8s.{ DockerImage, RunMode }
import me.lightspeed7.sk8s.manifests.Common.{ Java, Java11, Java8, Requests }
import play.api.libs.json._
import skuber.{ ConfigMap, EnvVar, ObjectMeta }

import scala.util.{ Failure, Success, Try }

final case class Sk8sAppConfig private (
    java: Java,
    name: String,
    namespace: String = "default",
    envVars: List[EnvVar] = List(),
    replicas: Int = 1,
    apiApp: Boolean = true,
    serviceAccountName: Option[String] = None,
    version: Common.Version = Common.Version(),
    image: Option[DockerImage] = None,
    request: Common.Requests = Common.Requests(),
    imagePullSecrets: String = Sk8sAppConfig.defaultDockerCreds,
    imagePullPolicy: String = "IfNotPresent" //
) {

  import com.softwaremill.quicklens._

  def replicas(cnt: Int): Sk8sAppConfig = this.copy(replicas = cnt)

  def major(major: Int): Sk8sAppConfig = this.modify(_.version.major).setTo(major)

  def minor(minor: Int): Sk8sAppConfig = this.modify(_.version.minor).setTo(minor)

  def patch(patch: Int): Sk8sAppConfig = this.modify(_.version.patch).setTo(patch)

  def image(image: DockerImage): Sk8sAppConfig = this.copy(image = Some(image))

  def imageTag(tag: String): Sk8sAppConfig =
    this.copy(image = image.map { img =>
      img.modify(_.tag).setTo(Some(tag))
    })

  def cpu(cpu: Double): Sk8sAppConfig = this.modify(_.request.cpu).setTo(cpu)

  def memory(memoryMb: Int): Sk8sAppConfig = this.modify(_.request.memoryMb).setTo(memoryMb)

  def isApiApp: Sk8sAppConfig = this.modify(_.apiApp).setTo(true)

  def isBackendApp: Sk8sAppConfig = this.modify(_.apiApp).setTo(false)

  def pullImageAlways: Sk8sAppConfig =
    this
      .modify(_.imagePullPolicy)
      .setTo("Always")

  def pullImageNevers: Sk8sAppConfig =
    this
      .modify(_.imagePullPolicy)
      .setTo("Never")

  def withServiceAccount(name: String): Sk8sAppConfig =
    this
      .modify(_.serviceAccountName)
      .setTo(Option(name))

  def withNoServiceAccountToken: Sk8sAppConfig =
    this
      .modify(_.serviceAccountName)
      .setTo(None)

  def writeToTopic(outputTopic: String): Sk8sAppConfig =
    this
      .modify(_.envVars)
      .using(in => in :+ createtEnvVar("WRITE_TO_TOPIC", outputTopic))

  def readFromTopic(topicName: String, pollingInternvalMs: Int = 50): Sk8sAppConfig =
    this
      .modify(_.envVars)
      .using(
        in =>
          in :+
          createtEnvVar("READ_FROM_TOPIC", topicName) :+
          createtEnvVar("KAFKA_POLLING_INTERVAL_MS", pollingInternvalMs.toString) //
      )

  def createtEnvVar(name: String, value: String): EnvVar = EnvVar(name, EnvVar.StringValue(value))

  def defaultVars: List[EnvVar] = {

    val javaOpts: EnvVar = java match {
      case Java8 =>
        createtEnvVar(
          "JAVA_OPTS",
          s"-server -Dpidfile.path=/dev/null -Djava.io.tmpdir=/opt/docker -Dnetworkaddress.cache.ttl=20 -Xms${request.memoryMb - 100}m -Xmx${request.memoryMb - 100}m  -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark"
        )
      case Java11 =>
        createtEnvVar("JAVA_OPTS",
                      "-server -Dpidfile.path=/dev/null -Djava.io.tmpdir=/opt/docker -Dnetworkaddress.cache.ttl=20 -XX:MaxRAMPercentage=75 ")
    }

    List(
      EnvVar("LOG_LEVEL", "INFO"),
      EnvVar(RunMode.SK8S_RUN_MODE_ENV, EnvVar.ConfigMapKeyRef("sk8s-run-mode", "sk8s-config")),
      EnvVar("HOST_IP", EnvVar.FieldRef("status.hostIP", "v1")),
      EnvVar("POD_IP", EnvVar.FieldRef("status.podIP", "v1"))
    ) :+ javaOpts
  }

  //
  // generators
  // ////////////////////////
  def createPodBase: Try[Pod] =
    if (image.isEmpty) {
      Failure(new IllegalStateException("No image provided in config"))
    } else {
      val livenessProbe  = Sk8sAppConfig.default8999Probe
      val readinessProbe = livenessProbe.copy(initialDelaySeconds = 10)

      val pod = Pod(this.name, namespace)(this)
        .withImagePullSecrets(imagePullSecrets)
        .withListenPort("backend-port", 8999)
        .withLivenessProbe(livenessProbe)
        .withReadinessProbe(readinessProbe)
        .withServiceAccount(serviceAccountName)

      if (apiApp)
        Success(pod.withListenPort("public-port", 9000))
      else
        Success(pod)
    }

  def createStatefulSetBase: Try[StatefulSet] =
    if (image.isEmpty) {
      Failure(new IllegalStateException("No image provided in config"))
    } else {

      val livenessProbe  = Sk8sAppConfig.default8999Probe
      val readinessProbe = livenessProbe.copy(initialDelaySeconds = 10)

      val sts = StatefulSet(this.name, namespace)(this)
        .withImagePullSecrets(imagePullSecrets)
        .withListenPort("backend-port", 8999)
        .withLivenessProbe(livenessProbe)
        .withReadinessProbe(readinessProbe)
        .withAntiAffinity(this)
        .withServiceAccount(serviceAccountName)

      if (apiApp)
        Success(sts.withListenPort("public-port", 9000))
      else
        Success(sts)
    }

  def createJobBase: Try[Job] = {

    val livenessProbe  = Sk8sAppConfig.default8999Probe
    val readinessProbe = livenessProbe.copy(initialDelaySeconds = 10)

    val job = Job(this.name, namespace)(this)
      .withImagePullSecrets(imagePullSecrets)
      .withLivenessProbe(livenessProbe)
      .withReadinessProbe(readinessProbe)
      .withServiceAccount(serviceAccountName)

    Success(job)
  }

  def createDeploymentBase: Try[Deployment] = {

    val livenessProbe  = Sk8sAppConfig.default8999Probe
    val readinessProbe = livenessProbe.copy(initialDelaySeconds = 10)

    val job = Deployment(this.name, namespace)(this)
      .withImagePullSecrets(imagePullSecrets)
      .withLivenessProbe(livenessProbe)
      .withReadinessProbe(readinessProbe)
      .withServiceAccount(serviceAccountName)
      .withListenPort("backend-port", 8999) // everyone has one

    if (apiApp)
      Success(job.withListenPort("public-port", 9000))
    else
      Success(job)
  }

  // map is [filename, data] in nature
  def createConfigMap(lines: Map[String, String]): Try[ConfigMap] = {

    val encoder = Base64.getEncoder

    val encoded: Map[String, String] = lines.map {
      case (k, v) =>
        (k + ".base64", encoder.encodeToString(v.getBytes()))
    }

    val cm = ConfigMap(
      metadata = ObjectMeta(name = version.fullName(name), namespace = namespace),
      data = encoded //
    )
    //
    Success(cm)
  }

}

object Sk8sAppConfig {

  import skuber.json._
  import skuber.json.format._

  implicit val __jsonVersion: OFormat[Common.Version]  = Json.format[Common.Version]
  implicit val __jsonRequests: OFormat[Requests]       = Json.format[Requests]
  implicit val __jsonDockerImage: OFormat[DockerImage] = Json.format[DockerImage]
  implicit val __json: OFormat[Sk8sAppConfig]          = Json.format[Sk8sAppConfig]

  private val defaultDockerCreds = "docker-hub-credentials"

  private val default8999Probe = Probes.HttpProbe(
    Probes.HTTPGet(8999, "/health", "HTTP"),
    initialDelaySeconds = 30,
    failureThreshold = 5,
    periodSeconds = 5,
    successThreshold = 1,
    timeoutSeconds = 2 //
  )

  def create(java: Java, name: String, namespace: String = "default"): Sk8sAppConfig =
    Sk8sAppConfig(java, name, namespace)

}
