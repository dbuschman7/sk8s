package me.lightspeed7.sk8s
import java.time.{ ZoneOffset, ZonedDateTime }
import java.util.UUID

import play.api.libs.json._

case class AppInfo(appName: String,
                   version: String,
                   buildTime: ZonedDateTime,
                   hostname: String = AppInfo.hostName,
                   ipAddress: String = AppInfo.ipAddress,
                   startTime: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC.normalized()),
                   appId: UUID = UUID.randomUUID()) {
  def toJson: JsObject                     = Json.toJson[AppInfo](this)(AppInfo._json).as[JsObject]
  def updateAppName(name: String): AppInfo = copy(appName = name)
}

object AppInfo extends EnvReads {

  implicit val _json: OFormat[AppInfo] = Json.format[AppInfo]

  val (hostName, ipAddress) = {
    val localhost = _root_.java.net.InetAddress.getLocalHost
    (localhost.getHostName, localhost.getHostAddress)
  }
}
