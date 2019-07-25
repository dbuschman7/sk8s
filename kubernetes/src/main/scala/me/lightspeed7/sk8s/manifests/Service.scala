package me.lightspeed7.sk8s.manifests

import play.api.libs.json.{ Json, OFormat }

case class Service(
    kind: String = "Service",
    apiVersion: String = "v1",
    metadata: Common.Metadata,
    spec: Service.Spec //
)

object Service {

  case class AppSelector(app: String)

  object AppSelector {
    implicit val __json: OFormat[AppSelector] = Json.format[AppSelector]
  }

  case class Port(port: Int, name: String)

  object Port {
    implicit val __json: OFormat[Port] = Json.format[Port]
  }

  case class Spec(ports: List[Port], selector: AppSelector)

  object Spec {
    implicit val __json: OFormat[Spec] = Json.format[Spec]
  }

  implicit val __json: OFormat[Service] = Json.format[Service]

  def labelSelector(
      name: String,
      namespace: Option[String],
      port: Int,
      portName: String //
  ): Service = {

    val meta: Common.Metadata = Common.Metadata(name, namespace, Some(Map[String, String]("app" -> name)), None)
    val selector: AppSelector = AppSelector(name)
    val spec: Spec            = Spec(List(Port(port, portName)), selector)
    Service(metadata = meta, spec = spec)
  }

}
