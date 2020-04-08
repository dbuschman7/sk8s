package me.lightspeed7.sk8s.util

import play.api.libs.json._

import scala.util.Random

case class AlphaId(id: String) {
  override def toString: String = id
}

object AlphaId {

  val __alphaIdWrites: Writes[AlphaId] = Writes[AlphaId] {
    case id: AlphaId => JsString(id.id)
    case unknown     => throw new IllegalArgumentException(s"Unknown VolumeRef - $unknown")
  }

  val __alphaIdReads: Reads[AlphaId] = new Reads[AlphaId] {
    def reads(json: JsValue): JsResult[AlphaId] = JsSuccess(AlphaId.fromString(json.as[JsString].value))
  }

  implicit val __volumeRefJson: Format[AlphaId] = Format(__alphaIdReads, __alphaIdWrites)
  implicit val __json: OFormat[AlphaId]         = Json.format[AlphaId]

  private val numericLetters: Seq[Char] = '0' to '9'
  private val upperLetters: Seq[Char]   = 'A' to 'Z'
  private val lowerLetters: Seq[Char]   = 'a' to 'z'
  private val allLetters: Seq[Char]     = upperLetters ++ lowerLetters

  val defaultLength = 20

  def randomLowerAlpha(length: Int = defaultLength): AlphaId = AlphaId(randomStringFromCharList(length, lowerLetters))

  def randomUpperAlpha(length: Int = defaultLength): AlphaId = AlphaId(randomStringFromCharList(length, upperLetters))

  def randomAlpha(length: Int = defaultLength): AlphaId = AlphaId(randomStringFromCharList(length, allLetters))

  private def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (_ <- 1 to length) {
      val randomNum = Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }

  def fromString(in: String): AlphaId = AlphaId(in.filter(allLetters.contains))

  /**
   * Alpha numeric id with no alpha in the first position
   */
  def randomAlphaWithNumerics(length: Int = defaultLength): AlphaId = {
    require(length >= 1)
    AlphaId(randomStringFromCharList(1, allLetters) ++ randomStringFromCharList(length - 1, allLetters ++ numericLetters))
  }

  def randomUpperAlphaWithNumerics(length: Int = defaultLength): AlphaId = {
    require(length >= 1)
    AlphaId(randomStringFromCharList(1, upperLetters) ++ randomStringFromCharList(length - 1, upperLetters ++ numericLetters))
  }

  def randomLowerAlphaWithNumerics(length: Int = defaultLength): AlphaId = {
    require(length >= 1)
    AlphaId(randomStringFromCharList(1, lowerLetters) ++ randomStringFromCharList(length - 1, lowerLetters ++ numericLetters))
  }

}

object AlphaNumericId {}
