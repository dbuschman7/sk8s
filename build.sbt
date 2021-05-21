import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerCommands
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.{ buildInfoKeys, buildInfoPackage }

name := "sk8s"
organization in ThisBuild := "me.lightspeed7"
version in ThisBuild := "0.8.0"

lazy val scala212 = "2.12.13"
//lazy val scala213 = "2.13.5"

scalaVersion := scala212

lazy val supportedScalaVersions = List(scala212)


licenses in ThisBuild += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

//
// PROJECTS
// ///////////////////////
lazy val global = project
  .in(file("."))
  .settings(settings)
  .settings(
    publishArtifact := false,
    skip in publish := true,
//    bintrayRelease := {},
    crossScalaVersions := supportedScalaVersions,
  )
  .disablePlugins(AssemblyPlugin)
  .aggregate(
    core,
    backend,
    play,
    kubernetes,
    slack,
    //
    plugin, // SBT plugins
    //
    templateBackend, // Example App
    templateApi // Example App
  )

lazy val core = project
  .settings(
    name := "sk8s-core",
    settings,
    assemblySettings,
    deploymentSettings,
    libraryDependencies ++= commonDependencies
  )
  .settings(testOptions in Test := Seq(Tests.Filter(harnessFilter)))
  .settings(testGrouping in Test := singleThreadedTests((definedTests in Test).value))

lazy val backend = project
  .settings(
    name := "sk8s-backend",
    settings,
    assemblySettings,
    deploymentSettings,
    libraryDependencies ++= commonDependencies :+ dependencies.sttpWithAkkHttp :+ dependencies.akkaHttp
  )
  .dependsOn(
    core       % "test->test;compile->compile"
  )

lazy val play = project
  .settings(
    name := "sk8s-play",
    settings,
    assemblySettings,
    deploymentSettings,
    libraryDependencies ++= commonDependencies ++ dependencies.playLibs ++ Seq(dependencies.javaxInject,
                                                                               dependencies.jwtPlayJson,
                                                                               dependencies.playJson)
  )
  .dependsOn(
    core       % "test->test;compile->compile",
    backend    % "test->test;compile->compile"
  )

lazy val kubernetes = project
  .settings(
    name := "sk8s-kubernetes",
    settings,
    assemblySettings,
    deploymentSettings,
    libraryDependencies ++= commonDependencies ++ Seq(dependencies.skuber, dependencies.parserCombinators, dependencies.quickLens)
  )
  .dependsOn(
    core % "test->test;compile->compile"
  )

//lazy val mongo = project
//  .settings(
//    name := "sk8s-mongodb",
//    settings,
//    assemblySettings,
//    deploymentSettings,
//    libraryDependencies ++= commonDependencies ++ Seq(dependencies.mongoScalaDB) :+ dependencies.ammoniteOps % "test"
//  )
//  .dependsOn(
//    core % "test->test;compile->compile"
//  )

//lazy val prometheus = project
//  .settings(
//    name := "sk8s-prometheus",
//    settings,
//    assemblySettings,
//    deploymentSettings,
//    libraryDependencies ++= commonDependencies ++ Seq(dependencies.prometheusClient, dependencies.prometheusClientProto, dependencies.akkaHttp)
//  )
//  .dependsOn(
//    core % "test->test;compile->compile"
//  )

lazy val slack = project
  .settings(
    name := "sk8s-slack",
    settings,
    assemblySettings,
    deploymentSettings,
    libraryDependencies ++= commonDependencies ++ Seq(dependencies.slack)
  )
  .dependsOn(
    core       % "test->test;compile->compile",
    kubernetes % "test->test;compile->compile"
  )

lazy val plugin = project
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-sk8s",
    description := "sbt plugin to generate build info and sk8s opinions",
    settings,
//    version := (version in ThisBuild).value + "-SNAPSHOT",
    sbtPlugin := true,
    publishMavenStyle := false,
    crossScalaVersions := Seq(scala212),
      //    bintrayRepository := "sbt-plugins",
//    bintrayOrganization in bintray := None,
    //
    scalacOptions := Seq("-Xfuture", "-unchecked", "-deprecation", "-feature", "-language:implicitConversions"),
    scalacOptions += "-language:experimental.macros",
    libraryDependencies += "org.scala-lang" % "scala-reflect"    % scalaVersion.value % Provided,
    libraryDependencies += "org.scala-sbt"  % "scripted-plugin_2.12" % sbtVersion.value,
    licenses := Seq("MIT License" -> url("https://github.com/sbt/sbt-buildinfo/blob/master/LICENSE")),
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value, "-Dsbt.log.noformat"),
    scriptedBufferLog := false
  )

//
// TEMPLATE APPS
// //////////////////////////
lazy val templateBackend = project
  .enablePlugins(JavaAppPackaging, BuildInfoPlugin, DockerPlugin)
  .settings(
    name := "backend",
    settings,
    assemblySettings,
    publishArtifact := false,
//    bintrayRelease := {},
    skip in publish := true,
//    skip in bintrayRelease := true,
    libraryDependencies ++= commonDependencies,
    buildInfoVars(name, version, scalaVersion, sbtVersion)
  )
  .dependsOn(
    backend % "test->test;compile->compile"
  )

lazy val templateApi = project
  .enablePlugins(PlayScala, BuildInfoPlugin, DockerPlugin)
  .settings(
    name := "api",
    settings,
    assemblySettings,
    publishArtifact := false,
//    bintrayRelease := {},
    skip in publish := true,
//    skip in bintrayRelease := true,
    libraryDependencies ++= commonDependencies :+ dependencies.scalaTestPlus,
    buildInfoVars(name, version, scalaVersion, sbtVersion)
  )
  .dependsOn(
    play % "test->test;compile->compile"
  )

//
// DEPENDENCIES
// //////////////////////////

lazy val dependencies =
  new {
    val akkaV               = "2.6.14" // 2.6.14
    val akkaHttpV           = "10.1.12" // 10.2.4
    val ammoniteOpsVer      = "2.3.8"
    val logbackV            = "1.2.3"
//    val mongodbScalaVersion = "2.9.0"
    val playV               = "2.7.9"
    val playJsonV           = "2.7.4"
    val sttpV               = "1.7.2"
    val scalaLoggingV       = "3.9.3"
    val scalatestV          = "3.1.4"
    val scalacheckV         = "1.14.3"
    val slf4jV              = "1.7.30"

    val ammoniteOps = "com.lihaoyi" %% "ammonite-ops" % ammoniteOpsVer

    val logback     = "ch.qos.logback"    % "logback-classic"       % logbackV withSources ()
    val akkaStream  = "com.typesafe.akka" %% "akka-stream"          % akkaV withSources ()
    val akkaHttp    = "com.typesafe.akka" %% "akka-http"            % akkaHttpV withSources ()
    val akkaSlf4j   = "com.typesafe.akka" %% "akka-slf4j"           % akkaV withSources ()
    val enumeratum  = "com.beachape"      %% "enumeratum-play-json" % "1.6.3" exclude ("org.scala-lang", "scala-library") withSources ()
    val javaxInject = "javax.inject"      % "javax.inject"          % "1" withSources ()

    val joda         = "joda-time"         % "joda-time"           % "2.10.10"
    val jwtPlayJson  = "com.pauldijou"     %% "jwt-play-json"      % "5.0.0"
//    val mongoScalaDB = "org.mongodb.scala" %% "mongo-scala-driver" % mongodbScalaVersion withSources ()

    val parserCombinators     = "org.scala-lang.modules"        %% "scala-parser-combinators" % "1.1.2" withSources ()
    val playJson              = "com.typesafe.play"             %% "play-json"                % playJsonV withSources () exclude ("org.scala-lang", "scala-library")
    val quickLens             = "com.softwaremill.quicklens"    %% "quicklens"                % "1.4.13" withSources ()
    val scalacheck            = "org.scalacheck"                %% "scalacheck"               % scalacheckV withSources ()
    val scalatest             = "org.scalatest"                 %% "scalatest"                % scalatestV withSources ()
    val scalaTestPlus         = "org.scalatestplus.play"        %% "scalatestplus-play"       % "4.0.3" % "test" withSources ()
    val scalaLogging          = "com.typesafe.scala-logging"    %% "scala-logging"            % scalaLoggingV withSources ()
    val slack                 = "com.github.slack-scala-client" %% "slack-scala-client"       % "0.2.16" withSources ()
    val slf4j                 = "org.slf4j"                     % "jcl-over-slf4j"            % slf4jV withSources ()
    val skuber                = "io.skuber"                     %% "skuber"                   % "2.6.0" withSources ()
    val sttpWithAkkHttp       = "com.softwaremill.sttp"         %% "akka-http-backend"        % sttpV withSources ()

    def playLibs: Seq[ModuleID] =
      Seq( //
        scalaTestPlus,
        "com.typesafe.play" %% "play-functional" % playJsonV withSources () exclude ("com.google.guava", "guava"),
        "com.typesafe.play" %% "play-guice"      % playV withSources () exclude ("com.google.guava", "guava"), //
        "com.typesafe.play" %% "filters-helpers" % playV withSources () exclude ("com.google.guava", "guava"),
        "com.typesafe.play" %% "play" /*     */  % playV withSources () exclude ("com.google.guava", "guava") exclude ("com.typesafe.akka", "akka-actor") exclude ("org.scala-lang", "scala-library"),
        "com.typesafe.play" %% "play-logback"    % playV withSources () exclude ("com.google.guava", "guava")
      )
  }

lazy val commonDependencies = Seq(
  dependencies.logback,
  // dependencies.logstash,
  dependencies.scalaLogging,
  dependencies.slf4j,
  // dependencies.typesafeConfig,
  dependencies.akkaStream,
  dependencies.akkaSlf4j,
  dependencies.playJson,
  dependencies.enumeratum,
  dependencies.joda,
  //
  dependencies.scalatest  % "test",
  dependencies.scalacheck % "test"
)

// SETTINGS

lazy val settings =
commonSettings ++
//wartremoverSettings ++
scalafmtSettings

lazy val compilerOptions = Seq(
  "-deprecation", //
  "-encoding",
  "UTF-8", //
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:reflectiveCalls"
  // "-language:existentials",
  // "-language:higherKinds",
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  scmInfo in ThisBuild := Some(ScmInfo(url("https://github.com/dbuschman7/sk8s.git"), "scm:git:https://github.com/dbuschman7/sk8s.git"))
//  bintrayReleaseOnPublish in ThisBuild := false
)

//lazy val wartremoverSettings = Seq(
//  wartremoverWarnings in (Compile, compile) ++= Warts.allBut(Wart.Throw)
//)

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )

lazy val assemblySettings = Seq(
  assemblyJarName in assembly := name.value + ".jar",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", _ @_*) => MergeStrategy.discard
    case "application.conf"          => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val deploymentSettings = Seq(
  publishArtifact in (Test, packageBin) := true, // Publish tests jarsproject
  publishArtifact in (Test, packageSrc) := true, // Publish tests-source jars
  publishArtifact in (Compile, packageDoc) := false, // Disable ScalaDoc generation
  publishArtifact in packageDoc := false,
  publishMavenStyle := true
  //
//  publishTo := publishDest // must use aliases to publish
)

//def publishDest: Option[Resolver] = Some("Some Realm" at "tbd")

def harnessFilter(name: String): Boolean = !(name endsWith "Harness")

def singleThreadedTests(definedTests: scala.Seq[TestDefinition]): scala.Seq[Tests.Group] = definedTests map { test =>
  Tests.Group(name = test.name, tests = Seq(test), runPolicy = Tests.SubProcess(ForkOptions()))
}

def buildInfoVars(name: SettingKey[String], version: SettingKey[String], scalaVersion: SettingKey[String], sbtVersion: SettingKey[String]) = {

  import scala.sys.process._

  def commit: String = ("git rev-parse --short HEAD" !!).trim

  def generateBuildInfo(name: BuildInfoKey, version: BuildInfoKey, scalaVersion: BuildInfoKey, sbtVersion: BuildInfoKey): Seq[BuildInfoKey] =
    Seq(name, version, scalaVersion, sbtVersion) :+ BuildInfoKey.action("buildTime") {
      System.currentTimeMillis
    } :+ BuildInfoKey.action("commit") {
      commit
    } :+ BuildInfoKey.action("branch") {
      branch
    } :+ BuildInfoKey.action("hasUnCommitted") {
      hasUnCommitted
    }

  def branch: String = ("git rev-parse --abbrev-ref HEAD" !!).trim

  def hasUnCommitted: Boolean = ("git diff-index --quiet HEAD --" !) != 0

  Seq(
    buildInfoPackage := "me.lightspeed7.sk8s",
    buildInfoKeys := generateBuildInfo(BuildInfoKey.action("name")(name.value), version, scalaVersion, sbtVersion)
  )
}

def dockerVars(
    name: SettingKey[String],
    baseImage: String = "sk8s-java-base:latest",
    backend: Boolean = false
) = {
  val ports = if (backend) { Seq(8999) } else { Seq(8999, 9000) }
  Seq(
    packageName in Docker := name.value,
    maintainer := "Dave Buschman",
    dockerBaseImage := baseImage,
    dockerExposedPorts := ports,
    dockerCommands += Cmd("ENV", "BACKEND_SERVER true")
  )
}
