package me.lightspeed7.sk8s.auth

import akka.http.scaladsl.model.DateTime
import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.JsonResult
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

//
// Controller Authentication Logic
// //////////////////////////////////////
trait AuthorizationLogic[TOKEN] {

  def components: ControllerComponents

  //s
  // sync
  // ////////////////////
  def authorizeAnySync[BODY](
    roles: Set[Role]
  )(action: AuthRequest[AnyContent, TOKEN] => Result)(implicit ctx: AuthContext[TOKEN]): Authorized[AnyContent, TOKEN] =
    Authorized(roles, components.parsers.default)(action)

  def authorizeSync[BODY](
    role: Role
  )(action: AuthRequest[AnyContent, TOKEN] => Result)(implicit ctx: AuthContext[TOKEN]): Authorized[AnyContent, TOKEN] =
    Authorized(Set(role), components.parsers.default)(action)

  //
  // async
  // ////////////////////
  def authorizeAny[BODY](roles: Set[Role])(
    action: AuthRequest[AnyContent, TOKEN] => Future[Result]
  )(implicit ctx: AuthContext[TOKEN]): AuthorizedAsync[AnyContent, TOKEN] =
    AuthorizedAsync(roles, components.parsers.default)(action)

  def authorize[BODY](role: Role)(
    action: AuthRequest[AnyContent, TOKEN] => Future[Result]
  )(implicit ctx: AuthContext[TOKEN]): AuthorizedAsync[AnyContent, TOKEN] =
    AuthorizedAsync(Set(role), components.parsers.default)(action)

}

class AuthRequest[BODY, TOKEN](val token: TOKEN, val ctx: AuthContext[TOKEN], request: Request[BODY])
    extends WrappedRequest[BODY](request) {
  val received: DateTime = DateTime.now
}

case class AuthorizedAsync[BODY, TOKEN](roles: Set[Role], parser: BodyParser[BODY])(
  action: AuthRequest[BODY, TOKEN] => Future[Result]
)(implicit
  val ctx: AuthContext[TOKEN]
) extends Action[BODY]
    with LazyLogging {

  implicit val executionContext: ExecutionContext = ctx.ec

  def apply(request: Request[BODY]): Future[Result] =
    CookieHandling.validateCookie(request, roles) match {
      case Right(token) =>
        val req = new AuthRequest[BODY, TOKEN](token, ctx, request)
        action(req)
      case Left(err) =>
        logger.warn("Auth Error - " + err)
        Future successful JsonResult.unauthorized("Auth Error").toResult
    }

}

case class Authorized[BODY, TOKEN](roles: Set[Role], parser: BodyParser[BODY])(action: AuthRequest[BODY, TOKEN] => Result)(
  implicit val ctx: AuthContext[TOKEN]
) extends Action[BODY]
    with LazyLogging {

  implicit val executionContext: ExecutionContext = ctx.ec

  def apply(request: Request[BODY]): Future[Result] =
    CookieHandling.validateCookie(request, roles) match {
      case Right(token) =>
        val req = new AuthRequest[BODY, TOKEN](token, ctx, request)
        Future successful action(req)
      case Left(err) =>
        logger.warn("Auth Error - " + err)
        Future successful JsonResult.unauthorized("Auth Error").toResult
    }

}
