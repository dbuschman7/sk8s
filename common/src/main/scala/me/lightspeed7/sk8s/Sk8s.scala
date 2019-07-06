package io.timeli.sk8s

import java.io.File
import java.nio.file.{ Path, Paths }

import akka.actor.{ ActorRef, ActorSystem }
import com.typesafe.scalalogging.LazyLogging
import io.timeli.sk8s.http.RestQuery
import io.timeli.sk8s.server.{ HealthStatusSummary, MemoryCronActor }
import io.timeli.sk8s.util.{ EnvironmentSource, FileUtils }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

object Sk8s extends FileUtils {

  def serviceAccount(basePath: Path = Paths.get("/var/run/secrets/")) = ServiceAccount(Paths.get(basePath.toString + "/kubernetes.io/serviceaccount"))

  def service(name: String): Service = Service(name.toUpperCase())

  def ku8sService: Service = service("kubernetes")

  def timeZone: String = exec("date +%Z")

  def secrets(name: String, encrypted: Boolean = false, mountPath: Path = Paths.get("/etc")): VolumeFiles = {
    VolumeFiles(name, mountPath, encrypted = encrypted)
  }

  def configMap(name: String, mountPath: Path = Paths.get("/etc")): VolumeFiles = {
    VolumeFiles(name, mountPath, encrypted = false)
  }

  def isKubernetes(basePath: Path = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token")): Boolean = new File(basePath.toString).exists()

  //
  object Kubernetes {

    val host: String = {
      lazy val k8s = EnvironmentSource.value("KUBERNETES_SERVICE_HOST", "kubernetes.default.svc.cluster.local")
      val isOSX = sys.props.getOrElse("os.name", "nope").contains("Mac OS X")
      if (isOSX) "10.0.0.1" else k8s
    }

    val port: Int = EnvironmentSource.valueInt("KUBERNETES_SERVICE_PORT", 443)

    val protocol: String = if (port == 443) "https" else "http"

    val getUrl: String = s"$protocol://$host:$port"
  }

  object MyPod {
    def ip: String = EnvironmentSource.value("MY_POD_IP", "0.0.0.0")

    def name: String = EnvironmentSource.value("MY_POD_NAME", "unknown")

    def namespace: String = EnvironmentSource.value("MY_POD_NAMESPACE", "unknown")

    def serviceDomainName(serviceName: String): String = Sk8s.serviceDomainName(serviceName, namespace)

    def podDomainName(podName: String): String = Sk8s.podDomainName(podName, namespace)
  }

  object MyCpu {
    def limit: Int = EnvironmentSource.valueInt("MY_CPU_LIMIT", 0)

    def request: Int = EnvironmentSource.valueInt("MY_CPU_REQUEST", 0)
  }

  object MyMemory {
    def limit: Int = EnvironmentSource.valueInt("MY_MEM_LIMIT", 0)

    def request: Int = EnvironmentSource.valueInt("MY_MEM_REQUEST", 0)
  }

  def serviceDomainName(serviceName: String, namespace: String): String = s"$serviceName.$namespace.svc.cluster.local"

  def podDomainName(podName: String, namespace: String): String = s"$podName.$namespace.pod.cluster.local"

  object HealthStatus {

    def good(name: String, value: Option[Double] = None): Unit = healthy(name, value)

    def healthy(name: String, value: Option[Double] = None): Unit = server.HealthStatus.healthy(name)

    def unhealthy(name: String, value: Option[Double] = None): Unit = server.HealthStatus.unhealthy(name)

    def isHealthy: Boolean = server.HealthStatus.isHealthy

    def summary: HealthStatusSummary = server.HealthStatus.summary
  }

  object MemoryCron {
    def startup(app: AppInfo, interval: Duration = 30 seconds)(implicit akka: ActorSystem): ActorRef = MemoryCronActor.startup(app, interval)
  }

}

final case class HostAndPort(host: String, port: Int) {
  def toUrl(protocol: String) = s"$protocol://$asString"

  def asString: String = s"$host:$port"
}

final case class ServiceAccount(basePath: Path) extends FileUtils with LazyLogging {

  lazy val isKubernetes: Boolean = new File(basePath.toString, "token").exists()

  lazy val namespace: String = getContents(basePath, "namespace").getOrElse("unknown")

  lazy val token: String = getContents(basePath, "token").getOrElse("unknown")

  lazy val crt: String = getContents(basePath, "ca.crt").getOrElse("unknown")

  def endpoints(serviceName: String, namespace: String = "default")(implicit ec: ExecutionContext): Future[Option[Endpoints]] = {
    RestQuery.queryForEndpoints(this, serviceName, namespace).flatMap {
      case Right(pts) => Future successful Some(pts)
      case Left(err) => err.code match {
        case 403 =>
          logger.warn(s"Service($namespace.$serviceName) - unauthorized access")
          Future successful None // unknowns are services not configured for this cluster, try the next one
        case 404 =>
          logger.warn(s"Service($namespace.$serviceName) - unknown service")
          Future successful None // unknowns are services not configured for this cluster, try the next one
        case _ => Future failed new Exception(err.toString)
      }
    }
  }
}

//
//  Service
// ////////////////////////////
final case class Service(name: String) {

  val host: String = EnvironmentSource.value(s"${name}_SERVICE_HOST").getOrElse("0.0.0.0")
  val port: Int = EnvironmentSource.valueInt(s"${name}_SERVICE_PORT").getOrElse(0)
  val secure: Boolean = port == 443
  val protocol: String = if (secure) "https" else "http"
  val getUrl: String = s"$protocol://$host:$port"
}

