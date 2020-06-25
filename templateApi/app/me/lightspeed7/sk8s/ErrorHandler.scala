package me.lightspeed7.sk8s

import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.mvc._

import scala.concurrent._

@Singleton
class ErrorHandler @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends play.api.http.HttpErrorHandler with LazyLogging {

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    import play.api.http.Status._
    val sc = statusCode
    Future.successful {
      logger.warn(s"Client Error ($statusCode) : $message $request")
      statusCode match {
        case BAD_REQUEST                          => JsonResult.globalOnBadRequest(message)
        case FORBIDDEN                            => JsonResult.globalOnUnauthorized
        case NOT_FOUND                            => JsonResult.globalOnHandlerNotFound
        case clientError if sc >= 400 && sc < 500 => JsonResult.globalOnBadRequest(message)
        case nonClientError =>
          throw new IllegalArgumentException(s"onClientError invoked with non client error status code $statusCode: $message")
      }
    }
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.successful(JsonResult.globalOnError(exception))

}
