package me.lightspeed7.sk8s.telemetry

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.{ AppInfo, Sk8s, Sk8sContext, Variables }
import org.lyranthe.prometheus.client.registry.{ ProtoFormat, TextFormat }
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{ Failure, Success }

class BackendServer(ipAddress: String = "0.0.0.0", port: Int = 8999, protobufFormet: Boolean)(implicit ctx: Sk8sContext) extends LazyLogging {

  import ctx._

  def pingPong: Route =
    path("ping") {
      get {
        complete(StatusCodes.OK -> "pong")
      }
    }

  def healthRoute: Route =
    path("health") {
      get {
        val summary = Sk8s.HealthStatus.summary
        if (summary.overall)
          complete(StatusCodes.OK)
        else
          complete(StatusCodes.ImATeapot -> summary.toJson.toString())
      }
    }

  def ipRoute: Route =
    path("ip") {
      get {
        complete(StatusCodes.OK -> ipAddress)
      }
    }

  def asText: String = {
    val buf = new StringBuilder("\n")
    Variables.dumpConfiguration({ in: String =>
      buf.append(in).append("\n")
    })

    buf.toString()
  }

  def asJson: JsValue = {
    val buf = new StringBuilder("\n")
    Variables.dumpJson({ in: String =>
      buf.append(in).append("\n")
    })(ctx.appInfo)

    Json.parse(buf.toString())
  }

  def configRoute: Route =
    path("config") {
      (get & extract(_.request.headers)) { requestHeaders =>
        val accepts: String = requestHeaders.find(h => h.is("Accept")).map(_.value().toLowerCase()).getOrElse("application/json").trim
        if (accepts == "text/plain") {
          complete(StatusCodes.OK -> asText) // respond with text
        } else if (accepts == "application/json") {
          val response = HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, asJson.toString()))
          complete(response) // respond with json
        } else {
          complete(StatusCodes.BadRequest -> "Unknown content type requested")
        }
      }
    }

  def metricsRoute(implicit appInfo: AppInfo): Route =
    path("metrics") {
      get {
        val data = TelemetryRegistry.snapshot

        val (ct, serialized) = if (protobufFormet) {
          (ContentType.parse(ProtoFormat.contentType).right.get, ProtoFormat.output(data))
        } else {
          (ContentType.parse(TextFormat.contentType).right.get, TextFormat.output(data))
        }

        complete(HttpResponse(entity = HttpEntity(ct, serialized)))
      }
    }

  val myExceptionHandler = ExceptionHandler {
    case _: ArithmeticException =>
      extractUri { uri =>
        logger.error(s"Request to $uri could not be handled normally")
        complete(HttpResponse(StatusCodes.InternalServerError, entity = "Unable to handle request"))
      }
  }

  val routes: Route = handleExceptions(myExceptionHandler) {
    Seq(pingPong, healthRoute, ipRoute, configRoute).foldLeft(metricsRoute(ctx.appInfo)) { case (prev, cur) => prev ~ cur }
  }

  logger.info(s"Http Server - $ipAddress:$port")
  Http()
    .bindAndHandle(routes, ipAddress, port)
    .onComplete {
      //
      case Success(binding) =>
        val address = binding.localAddress
        logger.info(s"Server is listening on ${address.getHostString}:${address.getPort}")

        // registerShutdownHook
        ctx.registerCloseable[AutoCloseable]("Http Server Shutdown", () => Await.result(binding.unbind(), 5 seconds))
      //
      case Failure(ex) =>
        logger.error(s"Server '${appInfo.appName}' could not be started", ex)
    }

}
