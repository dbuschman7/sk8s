package me.lightspeed7.sk8s.auth

import java.time.Clock

import me.lightspeed7.sk8s.{ Constant, Sk8sContext, Sources, Variable, Variables }

import scala.concurrent.ExecutionContext

case class AuthContext[TOKEN](jwtConfig: JwtConfigBase[TOKEN])(implicit ctx: Sk8sContext) {
  //
  implicit val ec: ExecutionContext = ctx.ec
  //
  implicit val clock: Clock = jwtConfig.clock

  val cookieNameVar: Variable[String] = Variables.firstValue[String](
    "JWT_COOKIE",
    Variables.maybeSource(Sources.sysProps, "jwt.cookie"),
    Variables.maybeSource(Sources.env, "JWT_COOKIE"),
    Constant("JWT_COOKIE")
  )

  lazy val cookieName: String = cookieNameVar.value

  val bearerTokenVar: Variable[Boolean] = Variables.firstValue[Boolean]( //
                                                                        "JWT_BEARER",
                                                                        Variables.maybeSource(Sources.sysProps, "jwt.bearer"),
                                                                        Variables.maybeSource(Sources.env, "JWT_BEARER"),
                                                                        Constant(true) //
  )

  val userBearerToken: Boolean = bearerTokenVar.value

}
