package me.lightspeed7.sk8s.auth

import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.{ Cookie, Request }

object CookieHandling extends LazyLogging {

  def makeCookie[TOKEN](token: TOKEN, age: Option[Long] = None)(implicit ctx: AuthContext[TOKEN]): Cookie = {
    val sessionAgeInSecs: Long = age.getOrElse(ctx.jwtConfig.sessionAgeInSeconds)

    Cookie(
      name = ctx.cookieName,
      value = ctx.jwtConfig.encode(token),
      domain = Some(ctx.jwtConfig.domain),
      maxAge = Some(sessionAgeInSecs.toInt),
      secure = true,
      httpOnly = true //
    )
  }

  def anonymousCookie[TOKEN](implicit ctx: AuthContext[TOKEN]): Cookie = makeCookie(ctx.jwtConfig.anonymous)

  //
  def validateCookie[A, TOKEN](request: Request[A], roles: Set[Role])(implicit ctx: AuthContext[TOKEN]): Either[String, TOKEN] =
    request.cookies.get(ctx.cookieName) match {
      case None         => Left("missing auth header")
      case Some(cookie) => ctx.jwtConfig.validate(cookie.value, roles)
    }

}
