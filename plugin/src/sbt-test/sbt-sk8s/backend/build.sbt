lazy val root = (project in file("."))
  .enablePlugins(Sk8sPlugin)
  .settings(
    name := "my-app-name",
    scalaVersion := "2.12.8",
    version := "0.1"
  )