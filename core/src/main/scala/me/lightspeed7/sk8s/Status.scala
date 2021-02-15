package me.lightspeed7.sk8s

import play.api.libs.json.{Json, OFormat}

final case class Details(name: String, kind: String)

object Details {
  implicit val __json: OFormat[Details] = Json.format[Details]
}

final case class ErrorMessage(kind: String,
                              apiVersion: String,
                              status: String,
                              message: String,
                              reason: String,
                              details: Option[Details],
                              code: Int
)

object ErrorMessage {
  implicit val __json: OFormat[ErrorMessage] = Json.format[ErrorMessage]
}
