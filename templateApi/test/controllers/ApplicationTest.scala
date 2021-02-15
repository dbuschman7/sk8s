package controllers

import me.lightspeed7.sk8s.auth.{AuthContext, CookieHandling, Role, RolesRegistry}
import me.lightspeed7.sk8s.backend.BackgroundTasks
import me.lightspeed7.sk8s.{EnvironmentSource, PlayServerFunSuite, Sources}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{Cookie, Result}
import play.api.test.{FakeRequest, Helpers}

import scala.concurrent.Future

class ApplicationTest extends PlayServerFunSuite with Matchers {

  Sources.env.asInstanceOf[EnvironmentSource].overrideVariable(BackgroundTasks.ServerStartName, "true")

  RolesRegistry.loadPresets()

  implicit val aCtx: AuthContext[JwtToken] = new AuthContextProvider(ctx).get

  val controller: Application = app.injector.instanceOf[Application]

  test("app can standup") {
    println("Success!")
  }

  test("secured endpoint") {
    val controller = new Application(ctx, Helpers.stubControllerComponents())

    val anonymousCookie: Cookie = CookieHandling.anonymousCookie

    val userRole: Role      = RolesRegistry.values().find(_.authorized).get
    val user: JwtUser       = controllers.JwtUser("curator@c.ai", "secret", "Curator", "User", Some(userRole.name))
    val userToken: JwtToken = controllers.JwtToken(user)
    val userCookie: Cookie  = CookieHandling.makeCookie(userToken)

    val req = FakeRequest.apply("Get", "/secured").withCookies(userCookie)

    val resp: Future[Result] = controller.secured(req)

    Helpers.status(resp) mustBe 200
  }

  override def beforeAllForSuite(): Unit = {}
}
