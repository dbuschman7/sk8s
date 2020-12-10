package me.lightspeed7.sk8s.backend

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s._
import me.lightspeed7.sk8s.server.JsonConfig

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

class BackendServer(ipAddress: String = "0.0.0.0", port: Int = 8999)(implicit ctx: Sk8sContext) extends LazyLogging {

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

  def configRoute: Route =
    path("config") {
      (get & extract(_.request.headers)) { requestHeaders =>
        val accepts: String = requestHeaders.find(h => h.is("accept")).map(_.value().toLowerCase()).getOrElse("application/json").trim
        if (accepts == "text/plain") {
          val asText = {
            val buf = new StringBuilder("\n")
            Variables.dumpConfiguration({ in: String =>
              buf.append(in).append("\n")
            })
            buf.toString()
          }
          complete(StatusCodes.OK -> asText) // respond with text
        } else if (accepts == "application/json") {
          val response = HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, JsonConfig.generate.toString()))
          complete(response) // respond with json
        } else {
          complete(StatusCodes.BadRequest -> "Unknown content type requested")
        }
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
    pingPong ~ healthRoute ~ ipRoute ~ configRoute
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
