package me.lightspeed7.sk8s.services

import me.lightspeed7.sk8s.AppInfo
import play.api.libs.json.{Json, OFormat}

object SystemProperties {

  def javaVersion: String = sys.props.get("java.version").getOrElse("unknown")

  def fromEnvironment(): SystemProperties = new SystemProperties(sys.props.get("java.class.path").getOrElse(""))
}

final class SystemProperties(rawData: String) {

  private val rawLines: Seq[String]           = rawData.split(":").toSeq
  private val commonLibSplitKeys: Set[String] = Set("maven2", "jcenter.bintray.com")

  def jarDependencies(extraSplitKeys: String*): Set[JarDependency] = {
    val allKeys = commonLibSplitKeys ++ extraSplitKeys

    rawLines.flatMap { l =>
      val key                     = allKeys.find(l.contains(_)).getOrElse("not.going.to.match")
      val jarPath: Option[String] = l.split(key).tail.headOption
      //      println(s"Raw - $l - $key - JarPath - $jarPath")
      jarPath.map(JarDependency.parse)
    }.toSet
  }

  def javaVersion: String = SystemProperties.javaVersion

  def appDependencies(extraSplitKeys: String*)(implicit appInfo: AppInfo): ApplicationDependencies =
    ApplicationDependencies(appInfo, jarDependencies(extraSplitKeys: _*))
}

case class JarDependency(group: String, artifact: String, version: String, scalaVersion: Option[String], jar: String)

object JarDependency {

  val sVersions: Set[String] = Set("_2.11", "_2.12", "_2.13")

  def parse(in: String): JarDependency = {

    def stripVersion(in: String): (String, String) =
      sVersions.find { v =>
        in.contains(v)
      } match {
        case None => ("", in)
        case Some(sVer) =>
          in.indexOf(sVer) match {
            case n if n < 1 => ("", in)
            case n          => (in.substring(n + 1), in.substring(0, n))
          }
      }

    val parts: Array[String] = in.split("/").reverse.filterNot(_.isEmpty)

    val jar                      = parts.head
    val version                  = parts.drop(1).head
    val (scalaVersion, artifact) = stripVersion(parts.drop(2).head)
    val group                    = parts.drop(3).toSeq.reverse.mkString(".")
    import me.lightspeed7.sk8s.util.String._
    JarDependency(group, artifact, version, scalaVersion.notBlank, jar)
  }

  implicit val __json: OFormat[JarDependency] = Json.format[JarDependency]
}

case class ApplicationDependencies(appInfo: AppInfo, dependencies: Set[JarDependency])

object ApplicationDependencies {
  implicit val __json: OFormat[ApplicationDependencies] = Json.format[ApplicationDependencies]

}
