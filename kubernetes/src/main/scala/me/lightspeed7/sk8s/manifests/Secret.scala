package me.lightspeed7.sk8s.manifests

import me.lightspeed7.sk8s.KeyValue
import play.api.libs.json.{Json, OFormat}
import sun.nio.ch.Secrets

case class Secret(kind: String = "Secret",
                  apiVersion: String = "vi",
                  metadata: Common.Metadata,
                  data: Secret.Data //
)

object Secret {

  case class Data(pairs: Seq[KeyValue])

  object Data {
    implicit val __jKV: OFormat[KeyValue] = Json.format[KeyValue]
    implicit val __json: OFormat[Data]    = Json.format[Data]
  }

  implicit val __json: OFormat[Secret] = Json.format[Secret]

  /**
    * apiVersion: v1
    * kind: Secret
    * metadata:
    *   name: mongo-prometheus
    *   namespace: default
    * data:
    *   username: foo
    *   password: bar
    */
  def opaque(name: String, namespace: Option[String], pairs: KeyValue*): Secret = {
    val meta: Common.Metadata = Common.Metadata(name, namespace, None, None)
    val data                  = Data(pairs)
    Secret(metadata = meta, data = data)
  }
}
