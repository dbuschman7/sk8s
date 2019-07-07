package me.lightspeed7.sk8s.manifests

import me.lightspeed7.sk8s.RunMode

object Sk8sConfigMap {

  def generateSk8sConfig(cluster: String, namespace: String, runMode: RunMode, extras: Map[String, String] = Map()): String = {

    val addedLines = extras.map { case (k, v) => s"  $k : $v" }.mkString("\n")

    s"""apiVersion: v1
      |kind: ConfigMap
      |metadata:
      |  name: sk8s-config
      |  namespace: $namespace|
      |data:
      |  cluster-name: $cluster
      |  sk8s-run-mode: ${runMode.name}
      |$addedLines
    """.stripMargin
  }

}
