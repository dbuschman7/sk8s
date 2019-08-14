package sbtsk8s

object FileGenerators {

  def geneateAppInfo(appName: String, appVersion: String, appBuild: String): String =
    s"""
         |package sk8s {
         |  object build {
         |    import java.time.format.DateTimeFormatter
         |    import java.time.ZonedDateTime
         |    import me.lightspeed7.sk8s.AppInfo
         |
         |    val appInfo = AppInfo(
         |      "$appName",
         |      "$appVersion",
         |      ZonedDateTime.parse("$appBuild", DateTimeFormatter.ISO_OFFSET_DATE_TIME)
         |    )
         |  }
         |}
         |""".stripMargin

}
