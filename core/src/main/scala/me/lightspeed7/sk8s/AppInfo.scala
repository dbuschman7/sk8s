package me.lightspeed7.sk8s
import java.util.UUID

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

case class AppInfo(appName: String,
                   version: String,
                   buildTime: DateTime,
                   hostname: String = AppInfo.hostName,
                   ipAddress: String = AppInfo.ipAddress,
                   startTime: DateTime = DateTime.now,
                   appId: UUID = UUID.randomUUID()) {
  def toJson: JsObject            = Json.toJson[AppInfo](this)(AppInfo._json).as[JsObject]
  def updateAppName(name: String) = copy(appName = name)
}

object AppInfo {

  //
  // Use independent DateTime formatting since this will be an open-source library
  // ///////////////////////////////////
  private val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  implicit val jodaDateReads: Reads[DateTime] =
    Reads[DateTime](js => js.validate[String].map[DateTime](dtString => DateTime.parse(dtString, DateTimeFormat.forPattern(dateFormat))))

  implicit val jodaDateWrites: Writes[DateTime] = (d: DateTime) => JsString(d.toString())

  implicit val appInfoDtFormat: Format[DateTime] = Format[DateTime](jodaDateReads, jodaDateWrites)

  implicit val _json: OFormat[AppInfo] = Json.format[AppInfo]

  val (hostName, ipAddress) = {
    val localhost = _root_.java.net.InetAddress.getLocalHost
    (localhost.getHostName, localhost.getHostAddress)
  }
}
