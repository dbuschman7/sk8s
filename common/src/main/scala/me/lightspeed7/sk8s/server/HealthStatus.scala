package io.timeli.sk8s.server

import java.util.concurrent.atomic.AtomicReference

import play.api.libs.json.{ JsObject, Json }

object HealthStatus {

  private[server] val currentHealth = new AtomicReference[HealthStatusSummary](HealthStatusSummary()) // holds the current health state

  private[sk8s] def isHealthy: Boolean = currentHealth.get().overall

  private[sk8s] def summary: HealthStatusSummary = currentHealth.get()

  private[sk8s] def healthy(name: String): Unit = {
    currentHealth.getAndUpdate(in => in.copy(states = in.states + (name -> HealthStatus(name, healthy = true))))
  }

  private[sk8s] def unhealthy(name: String): Unit = {
    currentHealth.getAndUpdate(in => in.copy(states = in.states + (name -> HealthStatus(name, healthy = false))))
  }
}

final case class HealthStatus(name: String, healthy: Boolean, value: Option[Double] = None) {

  private def snakify(tail: String): String = name.replaceAll(" ", "_").toLowerCase + "_" + tail

  def toJson: JsObject = {
    val vl: JsObject = value.map(v => Json.obj(snakify("value") -> v)).getOrElse(Json.obj())
    Json.obj(snakify("health") -> healthy) ++ vl
  }

}

final case class HealthStatusSummary(states: Map[String, HealthStatus] = Map()) {
  def overall: Boolean = states.values.forall(_.healthy)

  def toJson: JsObject = states.values.foldLeft(Json.obj("sk8s" -> "health", "overall_health" -> overall)) { case (acc, cur) => acc ++ cur.toJson }
}
