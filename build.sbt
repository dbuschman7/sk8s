import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys.{ buildInfoKeys, buildInfoPackage }

name := "sk8s"
organization in ThisBuild := "me.lightspeed7"
version in ThisBuild := "0.5.0"

scalaVersion in ThisBuild := "2.12.7"

//
// PROJECTS
// ///////////////////////
lazy val global = project
  .in(file("."))
  .settings(settings)
  .settings(
    publishArtifact := false,
    skip in publish := true
  )
  .disablePlugins(AssemblyPlugin)
  .aggregate(
    common,
    core,
    play,
    kubernetes,
    slack,
    //
    templateBackend,
    templateApi
  )

lazy val common = project
  .settings(
    name := "sk8s-common",
    settings,
    libraryDependencies ++= commonDependencies
  )
  .settings(testOptions in Test := Seq(Tests.Filter(harnessFilter)))
  .settings(testGrouping in Test := singleThreadedTests((definedTests in Test).value))
//
  .disablePlugins(AssemblyPlugin)

lazy val core = project
  .settings(
    name := "sk8s-core",
    settings,
    assemblySettings,
    deploymentSettings,
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(
    common % "test->test;compile->compile"
  )

lazy val play = project
  .settings(
    name := "sk8s-play",
    settings,
    assemblySettings,
    deploymentSettings,
    libraryDependencies ++= commonDependencies ++ dependencies.playLibs :+ dependencies.javaxInject
  )
  .dependsOn(
    common % "test->test;compile->compile",
    core
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
    common % "test->test;compile->compile",
    core
  )

lazy val slack = project
  .settings(
    name := "sk8s-slack",
    settings,
    assemblySettings,
    deploymentSettings,
    libraryDependencies ++= commonDependencies ++ Seq(dependencies.slack)
  )
  .dependsOn(
    common % "test->test;compile->compile",
    kubernetes,
    core
  )

//
// TEMPLATE APPS
// //////////////////////////
lazy val templateBackend = project
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "backend",
    settings,
    assemblySettings,
    publishArtifact := false,
    skip in publish := true,
    libraryDependencies ++= commonDependencies,
    buildInfoVars(name, version, scalaVersion, sbtVersion)
  )
  .dependsOn(
    core % "test->test;compile->compile"
  )

lazy val templateApi = project
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .settings(
    name := "api",
    settings,
    assemblySettings,
    publishArtifact := false,
    skip in publish := true,
    libraryDependencies ++= commonDependencies :+ dependencies.scalaTestPlus,
    buildInfoVars(name, version, scalaVersion, sbtVersion)
  )
  .dependsOn(
    common % "test->test;compile->compile",
    play
  )

//
// DEPENDENCIES
// //////////////////////////

lazy val dependencies =
  new {
    val logbackV = "1.2.3"
    // val logstashV        = "4.11"
    val scalaLoggingV = "3.7.2"
    val slf4jV        = "1.7.25"
    // val typesafeConfigV  = "1.3.1"
    // val pureconfigV      = "0.8.0"
    // val monocleV         = "1.4.0"
    val akkaV       = "2.5.23"
    val playV       = "2.6.23"
    val playJsonV   = "2.6.13"
    val sttpV       = "1.5.7"
    val scalatestV  = "3.0.4"
    val scalacheckV = "1.13.5"

    // val logstash       = "net.logstash.logback"       % "logstash-logback-encoder" % logstashV
    val logback               = "ch.qos.logback"                % "logback-classic"           % logbackV withSources ()
    val slf4j                 = "org.slf4j"                     % "jcl-over-slf4j"            % slf4jV withSources ()
    val akkaStream            = "com.typesafe.akka"             %% "akka-stream"              % akkaV withSources ()
    val akkaSlf4j             = "com.typesafe.akka"             %% "akka-slf4j"               % akkaV withSources ()
    val enumeratum            = "com.beachape"                  %% "enumeratum-play-json"     % "1.5.12" exclude ("org.scala-lang", "scala-library") withSources ()
    val javaxInject           = "javax.inject"                  % "javax.inject"              % "1" withSources ()
    val quickLens             = "com.softwaremill.quicklens"    %% "quicklens"                % "1.4.11" withSources ()
    val parserCombinators     = "org.scala-lang.modules"        %% "scala-parser-combinators" % "1.1.1" withSources ()
    val playJson              = "com.typesafe.play"             %% "play-json"                % playJsonV withSources () exclude ("org.scala-lang", "scala-library")
    val prometheusClient      = "org.lyranthe.prometheus"       %% "client"                   % "0.9.0-M5" withSources ()
    val prometheusClientProto = "org.lyranthe.prometheus"       %% "protobuf"                 % "0.9.0-M5" withSources ()
    val scalacheck            = "org.scalacheck"                %% "scalacheck"               % scalacheckV withSources ()
    val scalatest             = "org.scalatest"                 %% "scalatest"                % scalatestV withSources ()
    val scalaTestPlus         = "org.scalatestplus.play"        %% "scalatestplus-play"       % "3.1.2" % "test" withSources ()
    val scalaLogging          = "com.typesafe.scala-logging"    %% "scala-logging"            % scalaLoggingV withSources ()
    val slack                 = "com.github.slack-scala-client" %% "slack-scala-client"       % "0.2.6" withSources ()
    val skuber                = "io.skuber"                     %% "skuber"                   % "2.1.1" withSources ()
    val sttpWithAkkHttp       = "com.softwaremill.sttp"         %% "akka-http-backend"        % sttpV withSources ()

    def playLibs: Seq[ModuleID] = {
      val optional = Seq(
        "com.typesafe.play" %% "play-functional" % playJsonV withSources () exclude ("com.google.guava", "guava"),
        "com.typesafe.play" %% "play-guice"      % playV withSources () exclude ("com.google.guava", "guava") //
      )
      //
      Seq( //
        "com.typesafe.play" %% "filters-helpers" % playV withSources () exclude ("com.google.guava", "guava"),
        "com.typesafe.play" %% "play" /*     */  % playV withSources () exclude ("com.google.guava", "guava") exclude ("com.typesafe.akka", "akka-actor") exclude ("org.scala-lang", "scala-library"),
        "com.typesafe.play" %% "play-logback"    % playV withSources () exclude ("com.google.guava", "guava")
      ) ++ optional
    }
  }

lazy val commonDependencies = Seq(
  dependencies.logback,
  // dependencies.logstash,
  dependencies.scalaLogging,
  dependencies.slf4j,
  // dependencies.typesafeConfig,
  dependencies.akkaStream,
  dependencies.akkaSlf4j,
  dependencies.sttpWithAkkHttp,
  dependencies.playJson,
  dependencies.enumeratum,
  //
  dependencies.prometheusClient,
  dependencies.prometheusClientProto,
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
    "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    Resolver.jcenterRepo,
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/dbuschman7/sk8s/tree/release-" + version.value),
      "scm:git:https://github.com/dbuschman7/sk8s.git",
      Some("scm:git:https://github.com/dbuschman7sk8sgit")
    )),
  bintrayReleaseOnPublish in ThisBuild := false
)

//lazy val wartremoverSettings = Seq(
//  wartremoverWarnings in (Compile, compile) ++= Warts.allBut(Wart.Throw)
//)

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true,
    scalafmtTestOnCompile := true,
    scalafmtVersion := "1.2.0"
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
