package controllers

import javax.inject.Inject
import me.lightspeed7.sk8s.JsonResult.ok
import me.lightspeed7.sk8s.auth.{AuthContext, AuthorizationLogic, RolesRegistry}
import me.lightspeed7.sk8s.{ApplicationInfo, Sk8s, Sk8sContext}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._

class Application @Inject() (
  context: Sk8sContext,
  val components: ControllerComponents //
) extends AbstractController(components)
    with I18nSupport
    with AuthorizationLogic[JwtToken] {

  import context._
  implicit val aCtx: AuthContext[JwtToken] = AuthContext.apply(new JwtConfiguration())(context)

  def index: Action[AnyContent] =
    Action { _ =>
      val info = ApplicationInfo("App is running", appInfo.appName, appInfo.version, "Info", appInfo.startTime)
      val obj  = Json.toJson(info)
      ok(obj).toResult
    }

  def secured: Action[AnyContent] =
    authorizeAnySync(RolesRegistry.loggedIn) { _ =>
      val summary = Sk8s.HealthStatus.summary
      if (summary.overall)
        Ok(summary.toJson.toString)
      else
        ImATeapot(summary.toJson.toString)
    }

}
