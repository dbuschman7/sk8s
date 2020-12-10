package controllers

import akka.http.scaladsl.model.DateTime
import com.google.inject.{ Provider, Provides }
import javax.inject.Inject
import me.lightspeed7.sk8s.auth.{ AuthContext, JwtConfigBase, JwtResponse, Role }
import me.lightspeed7.sk8s.json.JsonImplicits
import me.lightspeed7.sk8s._
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{ JwtAlgorithm, JwtJsonImplicits }
import play.api.libs.json.{ JsResult, Json, OFormat }

import scala.util.Try

case class JwtUser(id: String, email: String, firstName: String, lastName: String, authorizedRole: Option[String])

case class JwtToken(user: JwtUser)

object JwtToken extends JsonImplicits {
  implicit val _user: OFormat[JwtUser]  = Json.format[JwtUser]
  implicit val _json: OFormat[JwtToken] = Json.format[JwtToken]
}

class JwtConfiguration extends JwtConfigBase[JwtToken] with JwtJsonImplicits {
  override def sessionAgeInSeconds: Long = 20 * 60 // 20 minutes

  override def domain(implicit ctx: AuthContext[JwtToken]): String = "foo.bar.com"

  override def anonymous(implicit ctx: AuthContext[JwtToken]): JwtToken = JwtToken(JwtUser("anonymous", "anon@bar.com", "Foo", "Bar", None))

  override def customRoleAuth(token: JwtToken, knowRoles: Set[Role]): Boolean = {
    val allowedSet: Set[String] = knowRoles.map(_.name)
    val tokenSet: Set[String]   = token.user.authorizedRole.map(i => Set(i)).getOrElse(Set())

    if (allowedSet.intersect(tokenSet).isEmpty) {
      logger.warn("Missing required role")
      return false
    }
    true
  }

  //
  // algorithms
  // //////////////////////

  val key: Variable[String] = Variables.firstValue[String](
    "JWT_KEY",
    Variables.maybeSource(Sources.sysProps, "sk8s.jwt.key"),
    Variables.maybeSource(Sources.env, "SK8S_JWT_KEY"),
    Constant("DevelopmentSecretDoNotUseInProd")
  )

  val issuer: Variable[String] = Variables.firstValue[String](
    "JWT_ISSUER",
    Variables.maybeSource(Sources.sysProps, "jwt.issuer"),
    Variables.maybeSource(Sources.env, "JWT_ISSUER"),
    Constant(validIssuer)
  )

  val audience: Variable[String] = Variables.firstValue[String](
    "JWT_AUDIENCE",
    Variables.maybeSource(Sources.sysProps, "jwt.audience"),
    Variables.maybeSource(Sources.env, "JWT_AUDIENCE"),
    Constant(validAudience)
  )

  val algorithms: Vector[JwtHmacAlgorithm] = Vector[JwtHmacAlgorithm](JwtAlgorithm.HS256, JwtAlgorithm.HS384, JwtAlgorithm.HS512)

  override def validIssuer: String   = "MalcolmReynolds"
  override def validAudience: String = "AuthorizedTest"
  override def signKey: String       = key.value

  def encode(content: JwtToken, expireTime: Option[DateTime] = None): String = {
    val json: String = Json.toJson(content).toString()
    fullEncode(json, expireTime, issuer.value, audience.value)
  }

  def decode(token: String): Try[JwtResponse[JwtToken]] = {
    def converter(in: String): JsResult[JwtToken] = Json.fromJson[JwtToken](Json.parse(in))
    fullDecode(token, converter)
  }

}

class AuthContextProvider @Inject()(appCtx: Sk8sContext) extends Provider[AuthContext[JwtToken]] {

  @Provides
  def get: AuthContext[JwtToken] =
    internalGet.getOrElse {
      val jwt = new JwtConfiguration()
      val ctx = AuthContext(jwt)(appCtx)

      internalGet = Some(ctx)
      ctx
    }

  private var internalGet: Option[AuthContext[JwtToken]] = None
}
