import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerCommands
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.{buildInfoKeys, buildInfoPackage}

name := "sk8s"


version in ThisBuild := {
  val versionFile = "version.txt"
  val valueStr ="0.8.1"

  // write to a file
  import java.io._
  val pw = new PrintWriter(new File(versionFile))
  pw.write(valueStr)
  pw.close()

  println("Version file -> " + valueStr)
  valueStr
}


lazy val scala212 = "2.12.13"
lazy val scala213 = "2.13.5"

lazy val scala3 = "3.0.0"

scalaVersion := scala3

lazy val supportedScalaVersions = List(scala212, scala213)

//
// PROJECTS
// ///////////////////////
lazy val global = project
  .in(file("."))
  .settings(
    publish / skip := true,
    publishArtifact := false,
    crossScalaVersions := Nil
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
    assemblySettings,
    deploymentSettings,
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= commonDependencies
  )
  .settings(testOptions in Test := Seq(Tests.Filter(harnessFilter)))
  .settings(testGrouping in Test := singleThreadedTests((definedTests in Test).value))

lazy val backend = project
  .settings(
    name := "sk8s-backend",
    assemblySettings,
    deploymentSettings,
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= commonDependencies :+ dependencies.sttpWithAkkHttp :+ dependencies.akkaHttp
  )
  .dependsOn(
    core       % "test->test;compile->compile"
  )

lazy val play = project
  .settings(
    name := "sk8s-play",
    assemblySettings,
    deploymentSettings,
    crossScalaVersions := supportedScalaVersions,
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
    assemblySettings,
    deploymentSettings,
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= commonDependencies ++ Seq(dependencies.skuber, dependencies.parserCombinators, dependencies.quickLens)
  )
  .dependsOn(
    core % "test->test;compile->compile"
  )

lazy val slack = project
  .settings(
    name := "sk8s-slack",
    assemblySettings,
    deploymentSettings,
    crossScalaVersions := supportedScalaVersions,
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
    sbtPlugin := true,
    crossScalaVersions := Seq(scala212),
    publishMavenStyle := false,
    //
    scalacOptions := Seq("-Xfuture", "-unchecked", "-deprecation", "-feature", "-language:implicitConversions"),
    scalacOptions += "-language:experimental.macros",
//    libraryDependencies += "org.scala-lang" % "scala-reflect"    % scalaVersion.value % Provided,
//    libraryDependencies += "org.scala-sbt"  % "scripted-plugin_2.12" % sbtVersion.value,
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
    assemblySettings,
    publishArtifact := false,
    publish / skip := true,
    crossScalaVersions := supportedScalaVersions,
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
    assemblySettings,
    publishArtifact := false,
    publish / skip := true,
    crossScalaVersions := supportedScalaVersions,
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
    val playJsonV           = "2.9.2"
    val sttpV               = "1.7.2"
    val scalaLoggingV       = "3.9.3"
    val scalatestV          = "3.1.4"
    val scalacheckV         = "1.14.3"
    val slf4jV              = "1.7.30"

    val ammoniteOps = ("com.lihaoyi" %% "ammonite-ops" % ammoniteOpsVer).cross(CrossVersion.for3Use2_13)

    val logback     = "ch.qos.logback"    % "logback-classic"       % logbackV withSources ()
    val akkaStream  = ("com.typesafe.akka" %% "akka-stream"          % akkaV withSources ()).cross(CrossVersion.for3Use2_13)
    val akkaHttp    = ("com.typesafe.akka" %% "akka-http"            % akkaHttpV withSources ()).cross(CrossVersion.for3Use2_13)
    val akkaSlf4j   = ("com.typesafe.akka" %% "akka-slf4j"           % akkaV withSources ()).cross(CrossVersion.for3Use2_13)
    val enumeratum  = ("com.beachape"      %% "enumeratum-play-json" % "1.6.3" withSources ()).cross(CrossVersion.for3Use2_13)
    val javaxInject = "javax.inject"      % "javax.inject"          % "1" withSources ()

    val joda         = "joda-time"         % "joda-time"           % "2.10.10"
    val jwtPlayJson  = ("com.pauldijou"     %% "jwt-play-json"      % "5.0.0").cross(CrossVersion.for3Use2_13)

    val parserCombinators     = ("org.scala-lang.modules"        %% "scala-parser-combinators" % "1.1.2" withSources ()).cross(CrossVersion.for3Use2_13)
    val playJson              = ("com.typesafe.play"             %% "play-json"                % playJsonV withSources () ).cross(CrossVersion.for3Use2_13)
    val quickLens             = ("com.softwaremill.quicklens"    %% "quicklens"                % "1.4.13" withSources ()).cross(CrossVersion.for3Use2_13)
    val scalacheck            = ("org.scalacheck"                %% "scalacheck"               % scalacheckV withSources ()).cross(CrossVersion.for3Use2_13)
    val scalatest             = ("org.scalatest"                 %% "scalatest"                % scalatestV withSources ()).cross(CrossVersion.for3Use2_13)
    val scalaTestPlus         = ("org.scalatestplus.play"        %% "scalatestplus-play"       % "4.0.3" % "test" withSources ()).cross(CrossVersion.for3Use2_13)
    val scalaLogging          = ("com.typesafe.scala-logging"    %% "scala-logging"            % scalaLoggingV withSources ()).cross(CrossVersion.for3Use2_13)
    val slack                 = ("com.github.slack-scala-client" %% "slack-scala-client"       % "0.2.16" withSources ()).cross(CrossVersion.for3Use2_13)
    val slf4j                 = "org.slf4j"                     % "jcl-over-slf4j"            % slf4jV withSources ()
    val skuber                = ("io.skuber"                     %% "skuber"                   % "2.6.0" withSources ()).cross(CrossVersion.for3Use2_13)
    val sttpWithAkkHttp       = ("com.softwaremill.sttp"         %% "akka-http-backend"        % sttpV withSources ()).cross(CrossVersion.for3Use2_13)

    def playLibs: Seq[ModuleID] =
      Seq( //
        scalaTestPlus,
        ("com.typesafe.play" %% "play-functional" % playJsonV withSources () exclude ("com.google.guava", "guava")).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.play" %% "play-guice"      % playV withSources () exclude ("com.google.guava", "guava")).cross(CrossVersion.for3Use2_13), //
        ("com.typesafe.play" %% "filters-helpers" % playV withSources () exclude ("com.google.guava", "guava")).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.play" %% "play" /*     */  % playV withSources () exclude ("com.google.guava", "guava") ).cross(CrossVersion.for3Use2_13),
        ("com.typesafe.play" %% "play-logback"    % playV withSources () exclude ("com.google.guava", "guava")).cross(CrossVersion.for3Use2_13)
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
scalacOptions ++= {
  Seq(
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-language:reflectiveCalls"
    // disabled during the migration
    // "-Xfatal-warnings"
  ) ++
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => Seq(
        "-unchecked",
        "-source:3.0-migration"
      )
      case _ => Seq(
        "-deprecation",
        "-Xfatal-warnings",
        "-Wunused:imports,privates,locals",
        "-Wvalue-discard"
      )
    })
}

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

//
// Publishing info
// ////////////////////////////////////
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
ThisBuild / publishTo := Some("Sonatype Snapshots Nexus" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
ThisBuild / publishMavenStyle := true
ThisBuild / scmInfo in ThisBuild := Some(ScmInfo(url("https://github.com/dbuschman7/sk8s.git"), "scm:git:https://github.com/dbuschman7/sk8s.git"))
ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / organization := "me.lightspeed7"
ThisBuild / organizationName := "me.lightspeed7"
ThisBuild / organizationHomepage := Some(url("https://dbuschman7.github.io"))

ThisBuild / homepage := Some(url("https://dbuschman7.github.io"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/dbuschman7/sk8s.git"),
    "scm:git:https://github.com/dbuschman7/sk8s.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "lightspeed7.me",
    name  = "David Buschman",
    email = "dbuschman7@gmail.com",
    url   = url("https://dbuschman7.github.io")
  )
)

ThisBuild / description := "Scala app support for running in Kubernetes"


//
// Settings setup
// ////////////////////////////
scalafmtOnCompile := true

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
)

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
