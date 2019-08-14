lazy val root = (project in file("."))
  .enablePlugins(Sk8sPlugin)
  .settings(
    name := "my-app-name",
    scalaVersion := "2.12.8",
    sk8sVersion := "0.6.2",
    sk8sPlayApp := false,
    version := "0.1"
  )