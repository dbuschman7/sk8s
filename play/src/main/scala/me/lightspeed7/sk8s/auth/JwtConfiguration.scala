package me.lightspeed7.sk8s.auth

import java.time.Clock

import akka.http.scaladsl.model.DateTime
import com.typesafe.scalalogging.LazyLogging
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtClaim, JwtJson}
import play.api.libs.json.{JsError, JsResult, JsSuccess}

import scala.util.{Failure, Success, Try}

case class JwtResponse[TOKEN](claim: JwtClaim, token: TOKEN)

trait JwtConfigBase[TOKEN] extends LazyLogging {

  def sessionAgeInSeconds: Long // Implement me

  def encode(in: TOKEN, expireTime: Option[DateTime] = None): String // Implement me

  def decode(token: String): Try[JwtResponse[TOKEN]] // Implement me

  def domain(implicit ctx: AuthContext[TOKEN]): String // Implement me

  def anonymous(implicit ctx: AuthContext[TOKEN]): TOKEN // Implement me

  final def validate(value: String, roles: Set[Role]): Either[String, TOKEN] =
    decode(value) match {
      case Failure(ex)                                     => Left(ex.getMessage)
      case Success(resp) if !isValid(resp.claim)           => Left("non-validated token provided")
      case Success(resp) if tokenIsAuthorized(resp, roles) => Right(resp.token)
      case Success(_)                                      => Left("non-validated token provided")
    }

  implicit val clock: Clock             = Clock.systemUTC
  def isValid(claim: JwtClaim): Boolean = claim.isValid(validIssuer) && claim.audience.exists(_.contains(validAudience))

  def tokenIsAuthorized(response: JwtResponse[TOKEN], userRoles: Set[Role]): Boolean = {
    if (!isValid(response.claim)) {
      logger.warn("Token invalid- bad claim")
      return false
    }

    customRoleAuth(response.token, userRoles)
  }

  def customRoleAuth(token: TOKEN, knownRoles: Set[Role]): Boolean // implement me

  //
  // standard config
  // ///////////////////////////
  def validIssuer: String
  def validAudience: String
  def signKey: String
  //
  // implementation
  // //////////////////////////
  val algorithms: Vector[JwtHmacAlgorithm]

  def pickOne: JwtHmacAlgorithm = algorithms(scala.util.Random.nextInt(algorithms.size))

  protected def fullEncode(json: String,                        //
                           expireTime: Option[DateTime] = None, //
                           issuer: String = validIssuer,        //
                           audience: String = validAudience     //
  ): String = {

    val expiresInSecs: Long = expireTime match {
      case Some(et) => (et.clicks - DateTime.now.clicks) / 1000
      case None     => sessionAgeInSeconds
    }

    val claim = JwtClaim(json)
      .by(validIssuer)
      .to(validAudience)
      .startsNow
      .issuedNow
      .expiresIn(expiresInSecs)

    JwtJson.encode(claim, signKey, pickOne)
  }

  protected def fullDecode(token: String, converter: String => JsResult[TOKEN]): Try[JwtResponse[TOKEN]] =
    JwtJson
      .decode(token, signKey, algorithms)
      .flatMap { claim: JwtClaim =>
        val tokenRes: JsResult[TOKEN] = converter(claim.content)

        tokenRes match {
          case JsSuccess(token, _) => Success(JwtResponse(claim, token))
          case JsError(errors)     => Failure(new Exception(errors.mkString("[ ", " ], [ ", " ]")))
        }
      }
}
