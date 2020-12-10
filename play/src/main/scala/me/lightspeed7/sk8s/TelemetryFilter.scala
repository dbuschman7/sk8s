package me.lightspeed7.sk8s

import akka.actor.ActorSystem
import akka.stream.{ Materializer, SystemMaterializer }
import com.google.inject.Inject
import me.lightspeed7.sk8s.telemetry.{ BasicCounter, TelemetryRegistry }
import play.api.mvc.{ Filter, RequestHeader, Result }

import scala.concurrent.{ ExecutionContext, Future }

class TelemetryFilter @Inject()(implicit val akka: ActorSystem, appInfo: AppInfo) extends Filter {

  implicit val mat: Materializer    = SystemMaterializer(akka).materializer
  implicit val ex: ExecutionContext = akka.dispatcher

  var requests: BasicCounter = TelemetryRegistry.counter("Requests")

  var badRequests: BasicCounter  = TelemetryRegistry.counter("BadRequests")
  var urlNotFound: BasicCounter  = TelemetryRegistry.counter("NotFoundRequests")
  var serverErrors: BasicCounter = TelemetryRegistry.counter("ErrorRequests")

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    requests.increment()

    nextFilter(requestHeader).map { result => // process the requests

      result.header.status match {
        case n if n == 400 => badRequests.increment()
        case n if n == 404 => urlNotFound.increment()
        case n if n >= 500 => serverErrors.increment()
        case _             => // nothing
      }

      //
      result
    }
  }
}
