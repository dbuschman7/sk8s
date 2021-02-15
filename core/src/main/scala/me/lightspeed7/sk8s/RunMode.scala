package me.lightspeed7.sk8s

import enumeratum._

//
// RunMode
// ///////////////////////
sealed abstract class RunMode(val name: String,
                              val description: String,
                              val requiresSecurity: Boolean,
                              val useSystemExit: Boolean = false
) extends EnumEntry
    with Serializable {
  final def isCurrent: Boolean = this == RunMode.currentRunMode
}

object RunMode extends Enum[RunMode] with PlayJsonEnum[RunMode] {

  import me.lightspeed7.sk8s.util.String._

  case object Test extends RunMode("Test", "Running inside a project build, maven or sbt", requiresSecurity = false)

  case object Developer extends RunMode("Developer", "Running on a developer box", requiresSecurity = false)

  case object FuncTest extends RunMode("Functest", "Running in the functional test environment", requiresSecurity = true)

  case object Staging
      extends RunMode("Staging", "Running in the staging environment", requiresSecurity = true, useSystemExit = true)

  case object Production
      extends RunMode("Production", "Running in the production environment", requiresSecurity = true, useSystemExit = true)

  //
  val values: scala.collection.immutable.IndexedSeq[RunMode] = findValues

  def find(in: String): Option[RunMode] =
    withNameInsensitiveOption(in) orElse in.notBlank.flatMap { in =>
      values.find { s: RunMode =>
        s.name.toLowerCase() == in.toLowerCase()
      }
    }

  val SK8S_RUN_MODE_PROP = "sk8s.run.mode"
  val SK8S_RUN_MODE_ENV  = "SK8S_RUN_MODE"

  def currentRunMode: RunMode =
    Seq(                                            // Order is critical
        Sources.sysProps.value(SK8S_RUN_MODE_PROP), //
        Sources.env.value(SK8S_RUN_MODE_ENV)        //
    ).flatten.headOption
      .flatMap(f => RunMode.find(f))
      .getOrElse(Developer)

  def setTest(): RunMode = setTestRunMode()

  def setTestRunMode(): RunMode = setRunMode(if (Sk8s.isKubernetes()) RunMode.FuncTest else RunMode.Test)

  def setRunMode(mode: RunMode): RunMode = {
    Sources.env.asInstanceOf[EnvironmentSource].overrideVariable(RunMode.SK8S_RUN_MODE_ENV, mode.name)
    mode
  }
}
