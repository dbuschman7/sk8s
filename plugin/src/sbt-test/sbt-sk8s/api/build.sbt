lazy val root = (project in file("."))
  .enablePlugins(PlayScala, Sk8sPlugin)
  .settings(
    name := "my-app-name",
    scalaVersion := "2.12.8",
    version := "0.1",
    sk8sVersion := "0.6.2",
    sk8sPlayApp := true,
    libraryDependencies +=  "me.lightspeed7" %% "sk8s-play" % sk8sVersion.value withSources ()
  )