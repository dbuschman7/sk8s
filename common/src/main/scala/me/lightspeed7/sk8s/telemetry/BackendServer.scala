package io.timeli.sk8s.telemetry

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import io.timeli.sk8s.util.Closeables
import io.timeli.sk8s.{ AppInfo, Sk8s }
import org.lyranthe.prometheus.client.registry.{ ProtoFormat, TextFormat }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.{ Failure, Success }

class BackendServer(ipAddress: String = "0.0.0.0", port: Int = 8999, protobufFormet: Boolean, configGen: => String)(implicit appInfo: AppInfo, system: ActorSystem) extends LazyLogging {

  implicit val ec: ExecutionContext = system.dispatcher

  implicit val mat: ActorMaterializer = {
    val mat = ActorMaterializer()
    Closeables.registerCloseable[AutoCloseable]("Http Server Shutdown", new AutoCloseable {
      override def close(): Unit = {
        mat.shutdown()
      }
    })
    mat
  }

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
      get {
        complete(StatusCodes.OK -> configGen)
      }
    }

  def metricsRoute(implicit appInfo: AppInfo): Route =
    path("metrics") {
      get {
        val data = io.timeli.sk8s.telemetry.TelemetryRegistry.snapshot

        val (ct, serialized) = if (protobufFormet) {
          (ContentType.parse(ProtoFormat.contentType).right.get, ProtoFormat.output(data))
        }
        else {
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
    Seq(pingPong, healthRoute, ipRoute, configRoute).foldLeft(metricsRoute) { case (prev, cur) => prev ~ cur }
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
        Closeables.registerCloseable[AutoCloseable]("Http Server Shutdown", new AutoCloseable {
          override def close(): Unit = {
            Await.result(binding.unbind(), 5 seconds)
          }
        })
      //
      case Failure(ex) =>
        logger.error(s"Server '${appInfo.appName}' could not be started", ex)
    }

}
