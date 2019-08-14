package sbtsk8s

import sbt._

trait Sk8sKeys {

  lazy val sk8sVersion = settingKey[String]("Specifies which version on sk8s jars to use")
  lazy val sk8sPlayApp = settingKey[Boolean]("Enables (true) or disables (false) play app configuration for a project.")
  lazy val sk8sAppInfo = taskKey[Unit]("Initializes the AppInfo Object with the build information")

  lazy val sk8sBaseAppPackage  = settingKey[String]("Specifies what the base Java package name for scaffolding")
  lazy val sk8sScaffoldBackend = taskKey[Unit]("Generates standard template files for the given project")
  lazy val sk8sScaffoldPlay    = taskKey[Unit]("Generates standard template files for the given project")
}
