package me.lightspeed7.sk8s.util

import java.util.UUID

@deprecated("Migrated to io.timeli.sk8s.AppInfo", "3.7.180612")
case class AppInfo(
    appName: String,
    version: String,
    buildTime: DateTime,
    hostname: String = AppInfoHelper.hostName,
    ipAddress: String = AppInfoHelper.ipAddress,
    startTime: DateTime = DateTime.now,
    appId: UUID = UUID.randomUUID()) {
  def updateAppName(name: String): AppInfo = copy(appName = name)
}

@deprecated("Migrated to io.timeli.sk8s.AppInfo", "3.7.180612")
private object AppInfoHelper {
  val (hostName, ipAddress) = {
    val localhost = java.net.InetAddress.getLocalHost
    (localhost.getHostName, localhost.getHostAddress)
  }
}
