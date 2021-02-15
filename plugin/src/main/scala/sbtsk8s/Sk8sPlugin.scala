package sbtsk8s

import java.io.File
import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

import sbt.{Def, _}
import Keys.{libraryDependencies, _}

object Sk8sPlugin extends AutoPlugin {

  override val trigger: PluginTrigger = noTrigger // allRequirements
  override val requires: Plugins      = plugins.JvmPlugin

  object autoImport extends Sk8sKeys

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    sk8sPlayApp := false,
    sk8sAppInfo := appInfoTask.value,
    (sourceGenerators in Compile) ++= Seq(appInfoTask.taskValue),
    libraryDependencies ++= sk8sDeps(sk8sPlayApp.value, sk8sVersion.value)
  )

  private def appInfoTask: Def.Initialize[Task[Seq[File]]] =
    Def.task {
      implicit val log: Logger = sLog.value
      //

      //
      val appBuild: String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC.normalized()))
      val sk8sBuildFile    = new File((sourceManaged in Compile).value.getAbsolutePath, "Sk8sAppInfo.scala")
      log.info(s"Generating AppInfo file(${sk8sPlayApp.value}) - $sk8sBuildFile ... ")
      writeFile(sk8sBuildFile)(FileGenerators.geneateAppInfo(name.value, version.value, appBuild))
      //
      Seq(sk8sBuildFile)
    }

  //
  // Helpers
  // //////////////////////
  def sk8sDeps(playApp: Boolean, sk8sVersion: String): Seq[ModuleID] =
    Seq(
      "me.lightspeed7" %% "sk8s-core" % sk8sVersion withSources (),
      "me.lightspeed7" %% "sk8s-core" % sk8sVersion % "test" classifier "tests" withSources ()
    ) ++
    (if (playApp) {
       Seq(
         "me.lightspeed7" %% "sk8s-play" % sk8sVersion withSources (),
         "me.lightspeed7" %% "sk8s-play" % sk8sVersion % "test" classifier "tests" withSources ()
       )
     } else { Seq() })

  def writeFile(file: File, overwrite: Boolean = false)(contents: => String)(implicit log: Logger): Unit = {
    val path = file.getAbsolutePath
    if (file.exists() && !overwrite) {
      log.info(s"Sk8s - File exists   - $path")
    } else {
      log.info(s"Sk8s - Writing file  - $path ...")
      IO.write(file, contents, append = false)
    }
  }

}
