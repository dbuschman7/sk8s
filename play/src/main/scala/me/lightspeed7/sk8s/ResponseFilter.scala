package me.lightspeed7.sk8s

import akka.actor.ActorSystem
import akka.stream.{Materializer, SystemMaterializer}
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import me.lightspeed7.sk8s.telemetry.{BasicCounter, BasicTimer, TelemetryRegistry}
import play.api.mvc.{Filter, RequestHeader, ResponseHeader, Result}
import play.api.routing.Router

import scala.concurrent.{ExecutionContext, Future}
import scala.util._

class ResponseFilter @Inject() (implicit val akka: ActorSystem, appInfo: AppInfo) extends Filter with LazyLogging {

  implicit val mat: Materializer    = SystemMaterializer(akka).materializer
  implicit val ex: ExecutionContext = akka.dispatcher

  val exclusionPaths: Seq[String]  = Seq("/", "/favicon.ico")
  val exclusionStarts: Seq[String] = Seq("/telemetry", "/health", "/assets", "/webjars", "/metrics", "/ip", "/ping", "/config")

  lazy val requests: BasicCounter          = TelemetryRegistry.counter("requests")
  lazy val requestTimes: BasicTimer        = TelemetryRegistry.latency("request_time")
  lazy val requestTimesFailure: BasicTimer = TelemetryRegistry.latency("request_time_failures")

  // This will need to be better at performance eventually
  def logResponse(action: String, requestHeader: RequestHeader, responseHeader: ResponseHeader, requestTime: Long): Unit = {

    val found: Boolean = {
      val requestPath        = requestHeader.path
      val fullPaths: Boolean = exclusionPaths.contains(requestPath)
      val startsWith: Boolean = exclusionStarts
        .map { es =>
          requestPath.startsWith(es)
        }
        .foldLeft(false) { (a, b) =>
          a || b
        }

      fullPaths || startsWith
    }

    if (!found) {
      val origin = Try(requestHeader.headers("Origin")).toOption.getOrElse("unknown")
      logger.info(s"($origin)$action took ${requestTime}ms on path ${requestHeader.path} and returned ${responseHeader.status}")
      requests.increment()
      requestTimes.update(requestTime)
      if (action == "failure") {
        requestTimesFailure.update(requestTime)
      }
    }
  }

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result => // process the requests

      val endTime     = System.currentTimeMillis
      val requestTime = endTime - startTime

      if (!requestHeader.attrs.contains(Router.Attrs.HandlerDef)) {
        result
      } else {
        val handlerDef             = requestHeader.attrs(Router.Attrs.HandlerDef)
        val actionTry: Try[String] = Try(handlerDef.controller + "." + handlerDef.method)
        actionTry match {
          case Failure(_)      => logResponse("failure", requestHeader, result.header, requestTime)
          case Success(action) => logResponse(action, requestHeader, result.header, requestTime)
        }
        result.withHeaders("Request-Time" -> requestTime.toString, "X-Timeli-Version" -> appInfo.version)
      }
    }
  }

}
