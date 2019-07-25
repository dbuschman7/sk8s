package me.lightspeed7.sk8s.manifests

import me.lightspeed7.sk8s.manifests.Common.Selector
import play.api.libs.json.{ Json, OFormat }

case class Budget(kind: String = "PodDisruptionBudget", apiVersion: String = "policy/v1beta1", metadata: Common.Metadata, spec: Budget.Spec //
)

object Budget {

  case class Spec(selector: Selector, minAvailable: Option[Int] = None, maxUnavailable: Option[Int] = None)

  object Spec {
    implicit val __json: OFormat[Spec] = Json.format[Spec]
  }

  implicit val __json: OFormat[Budget] = Json.format[Budget]

  def labelSelector(
      name: String,
      namespace: Option[String],
      minAvailable: Option[Int] = None,
      maxUnavailable: Option[Int] = None //
  ): Budget = {

    val meta: Common.Metadata = Common.Metadata(name, namespace, None, None)
    val selector: Selector    = Selector(Some(Map[String, String]("app" -> name)))
    val lspec: Spec           = Spec(selector, minAvailable, maxUnavailable)
    Budget(metadata = meta, spec = lspec)
  }

}
