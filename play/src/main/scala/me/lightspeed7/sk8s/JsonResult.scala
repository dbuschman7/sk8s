package me.lightspeed7.sk8s

import java.time.ZonedDateTime

import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.json.JsonImplicits

import play.api.libs.json.{ JsValue, Json, OFormat }
import play.api.mvc.{ Result, Results }

import scala.concurrent.ExecutionContext

case class ApplicationInfo(message: String, app: String, version: String, reason: String, startTime: ZonedDateTime)

object ApplicationInfo extends JsonImplicits {
  implicit val _json: OFormat[ApplicationInfo] = Json.format[ApplicationInfo]
}

case class JsonResult(code: Int, message: JsValue) {
  val responseType = "application/json"

  def toResult: Result = new Results.Status(code)(message.toString()).as(responseType)
}

final case class MessageResponse(reason: String, message: String)

object MessageResponse extends JsonImplicits {
  implicit val _json: OFormat[MessageResponse] = Json.format[MessageResponse]
}

object JsonResult extends LazyLogging {

  implicit val _format: OFormat[JsonResult] = Json.format[JsonResult]

  private def toEmpty(in: String, reason: String): JsValue = Json.toJson(MessageResponse(reason, Option(in).getOrElse(reason)))

  def fromCode(code: Int, message: String) = JsonResult(code, toEmpty(message, "Info"))

  def ok(json: JsValue): JsonResult = JsonResult(200, json)

  def ok(message: String): JsonResult = ok(toEmpty(message, "Info"))

  def created(json: JsValue): JsonResult = JsonResult(201, json)

  def created(message: String): JsonResult = created(toEmpty(message, "Info"))

  def accepted(json: JsValue): JsonResult = JsonResult(202, json)

  def accepted(message: String): JsonResult = accepted(toEmpty(message, "Info"))

  def deprecated(json: JsValue): JsonResult = JsonResult(299, json)

  def deprecated(message: String): JsonResult = deprecated(toEmpty(message, "Info"))

  def badRequest(json: JsValue): JsonResult = JsonResult(400, json)

  def badRequest(message: String): JsonResult = badRequest(toEmpty(message, "Info"))

  def unauthorized(json: JsValue): JsonResult = JsonResult(401, json)

  def unauthorized(message: String): JsonResult = unauthorized(toEmpty(message, "Info"))

  def forbidden(json: JsValue): JsonResult = JsonResult(403, json)

  def forbidden(message: String): JsonResult = forbidden(toEmpty(message, "Info"))

  def notFound(json: JsValue): JsonResult = JsonResult(404, json)

  def notFound(message: String): JsonResult = notFound(toEmpty(message, "ObjectNotFound"))

  def conflict(json: JsValue): JsonResult = JsonResult(409, json)

  def conflict(message: String): JsonResult = conflict(toEmpty(message, "Info"))

  def error(ex: Throwable): JsonResult = error(ex.getMessage)

  def error(ex: JsValue): JsonResult = JsonResult(500, ex)

  def error(message: String): JsonResult = error(toEmpty(message, "Info"))

  def serviceUnavailable(json: JsValue): JsonResult = JsonResult(503, json)

  def globalOnError(ex: Throwable)(implicit ec: ExecutionContext): Result = {
    logger.error("globalOnError", ex)
    error(Json.toJson(MessageResponse("Info", ex.getMessage))).toResult
  }

  def globalOnHandlerNotFound(implicit ec: ExecutionContext): Result =
    notFound(Json.toJson(MessageResponse("BadUrl", "URI Not Found"))).toResult

  def globalOnUnauthorized(implicit ec: ExecutionContext): Result =
    unauthorized(Json.toJson(MessageResponse("Info", "Unauthorized"))).toResult

  def globalOnBadRequest(error: String)(implicit ec: ExecutionContext): Result = {
    logger.error("globalOnBadRequest - " + error)
    badRequest(Json.toJson(MessageResponse("BadUrl", error))).toResult
  }

  def applicationRoot(implicit ec: ExecutionContext, appInfo: AppInfo): Result = {
    val info = ApplicationInfo("App is running", appInfo.appName, appInfo.version, "Info", appInfo.startTime)
    val obj  = Json.toJson(info)
    ok(obj).toResult
  }
}
