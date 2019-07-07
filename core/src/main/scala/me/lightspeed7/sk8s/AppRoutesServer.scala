package me.lightspeed7.sk8s

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.util.Closeables

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

case class ServerConfig(routes: Seq[Route] = Seq.empty, interface: String = "0.0.0.0", port: Int = 9000)

class AppRoutesServer(app: BackendApplication, config: ServerConfig)(implicit appCtx: Sk8sContext) extends LazyLogging {

  import appCtx._

  private def healthRoute: Route =
    path("health") {
      get {
        val summary = Sk8s.HealthStatus.summary
        if (summary.overall)
          complete(StatusCodes.OK)
        else
          complete(StatusCodes.ImATeapot -> summary.toJson.toString())
      }
    }

  private def appInfoRoute(implicit appCtx: Sk8sContext): Route =
    pathEndOrSingleSlash {
      get {
        complete(StatusCodes.OK -> appCtx.appInfo.toJson.toString())
      }
    }

  private val myExceptionHandler = ExceptionHandler {
    case _: ArithmeticException =>
      extractUri { uri =>
        logger.error(s"Request to $uri could not be handled normally")
        complete(HttpResponse(StatusCodes.InternalServerError, entity = "Unable to handle request"))
      }
  }

  private val routes: Route = handleExceptions(myExceptionHandler) {
    (healthRoute +: config.routes).foldLeft(appInfoRoute) { case (prev, cur) => prev ~ cur }
  }

  logger.info(s"Http Server - ${config.interface}:${config.port}")
  Http()
    .bindAndHandle(routes, config.interface, config.port)
    .onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        logger.info(s"Server is listening on ${address.getHostString}:${address.getPort}")

        // registerShutdownHook
        Closeables.registerCloseable[AutoCloseable]("Http Server Shutdown", new AutoCloseable {
          override def close(): Unit =
            Await.result(binding.unbind(), 5 seconds)
        })
      //
      case Failure(ex) =>
        logger.error(s"Server '${appInfo.appName}' could not be started", ex)
        app.shutdown(ex)
    }
}
