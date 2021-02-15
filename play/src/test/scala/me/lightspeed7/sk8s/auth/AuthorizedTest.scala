package me.lightspeed7.sk8s.auth

import me.lightspeed7.sk8s._
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Cookie
import play.api.test.FakeRequest

import scala.util.Try

class AuthorizedTest extends Sk8sFunSuite with Matchers {

  RolesRegistry.loadPresets()

  implicit val aCtx: AuthContext[JwtToken] = AuthContext(new JwtConfiguration())(ctx)

  //
  //
  // Tokens
  // //////////////////////////////////

  val anonymousCookie: Cookie = CookieHandling.anonymousCookie

  val userRole: Role      = RolesRegistry.values().find(_.authorized).get
  val user: JwtUser       = JwtUser("curator@c.ai", "secret", "Curator", "User", Some(userRole.name))
  val userToken: JwtToken = JwtToken(user)
  val userCookie: Cookie  = CookieHandling.makeCookie(userToken)

  //
  //
  //
  // /////////////////////////////////
  test("encode decode successfully") {
    val cfg = aCtx.jwtConfig

    val encode: String                       = cfg.encode(userToken, None)
    val response: Try[JwtResponse[JwtToken]] = cfg.decode(encode)
    println(response)

    response.isSuccess mustBe true

    response.get.claim.isValid(cfg.clock) mustBe true
    response.get.claim.issuer.contains(cfg.validIssuer) mustBe true
    cfg.isValid(response.get.claim) mustBe true

    cfg.tokenIsAuthorized(response.get, Set(userRole)) mustBe true

  }

  test("encode decode anonymous") {
    val cfg = aCtx.jwtConfig

    val encode: String                       = cfg.encode(cfg.anonymous, None)
    val response: Try[JwtResponse[JwtToken]] = cfg.decode(encode)
    println(response)

    response.isSuccess mustBe true

    response.get.claim.isValid(cfg.clock) mustBe true
    response.get.claim.issuer.contains(cfg.validIssuer) mustBe true
    cfg.isValid(response.get.claim) mustBe true

    cfg.tokenIsAuthorized(response.get, Set(userRole)) mustBe false

  }

  test("request header test") {
    val req = FakeRequest.apply("PUT", "/foo").withCookies(userCookie)

    val result: Either[String, JwtToken] = CookieHandling.validateCookie(req, Set(userRole))
    result.isRight mustBe true
    result.right.get mustBe userToken

  }

  test("failed role auth") {
    val req = FakeRequest.apply("PUT", "/foo").withCookies(anonymousCookie)

    val resultFail: Either[String, JwtToken] = CookieHandling.validateCookie(req, Set(userRole))
    resultFail.isLeft mustBe true
    resultFail.left.get mustBe "non-validated token provided"

  }
}
