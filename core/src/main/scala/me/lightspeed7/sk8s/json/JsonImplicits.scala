package me.lightspeed7.sk8s.json

import java.nio.file.{ Path, Paths }
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.joda.time.{ DateTime, DateTimeZone, Period }
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatterBuilder, DateTimeParser }
import play.api.libs.json.{ Format, JsError, JsNumber, JsResult, JsString, JsSuccess, JsValue, JsonValidationError, Reads, Writes }

import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.util.{ Failure, Success, Try }

object JsonImplicits extends JsonImplicits

trait JsonImplicits {

  //
  // Path
  // ///////////////////////////
  implicit val pathReads: Reads[Path] = new Reads[Path] {
    def reads(json: JsValue): JsResult[Path] = json match {
      case JsString(s) =>
        Option(s) match {
          case Some(s) =>
            Try(Paths.get(s)) match {
              case Success(p)  => JsSuccess(p)
              case Failure(ex) => JsError(s"${ex.getMessage} : '${json.toString()}'")
            }
          case None => JsError(s"Invalid value given for Path : '${json.toString()}'")
        }
      case _ => JsError("String value expected")
    }
  }

  implicit val pathWrites: Writes[Path] = new Writes[Path] {
    def writes(id: Path): JsValue = JsString(id.toString)
  }

  implicit val pathFormat: Format[Path] = Format(pathReads, pathWrites)

  //
  // UUID format
  // ///////////////////////////
  implicit val uidReads: Reads[UUID] = new Reads[UUID] {
    def reads(json: JsValue): JsResult[UUID] = json match {
      case JsString(s) =>
        Option(s) match {
          case Some(s) => JsSuccess(UUID.fromString(s))
          case None    => JsError(s"Invalid value given for UUID : '${json.toString()}'")
        }
      case _ => JsError("String value expected")
    }
  }

  implicit val uidWrites: Writes[UUID] = new Writes[UUID] {
    def writes(id: UUID): JsValue = JsString(id.toString)
  }

  implicit val uidFormat: Format[UUID] = Format(uidReads, uidWrites)

  //
  // Joda DateTimeZone format
  // ////////////////////////////
  implicit val dtzReads: Reads[DateTimeZone] = new Reads[DateTimeZone] {
    def reads(json: JsValue): JsResult[DateTimeZone] = json match {
      case JsString(s) =>
        Option(s) match {
          case Some(s) => JsSuccess(DateTimeZone.forID(s))
          case None    => JsError(s"Invalid value given for timeZone : '${json.toString()}'")
        }
      case _ => JsError("String value expected")
    }
  }

  implicit val dtzWrites: Writes[DateTimeZone] = new Writes[DateTimeZone] {
    def writes(timeZone: DateTimeZone): JsValue = JsString(timeZone.getID)
  }

  implicit val dtzFormat: Format[DateTimeZone] = Format(dtzReads, dtzWrites)

  //
  // Joda DateTime format
  //////////////////////////////
  implicit val dtReads: Reads[DateTime] = new Reads[DateTime] {
    val parsers = Array[DateTimeParser](
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ").getParser,
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ").getParser,
      DateTimeFormat.forPattern("yyyy-MM-dd").getParser,
      DateTimeFormat.forPattern("yyyy-MM-ddZZ").getParser,
      DateTimeFormat.forPattern("MM-dd-yyyy HH:mm:ss.SSSZ").getParser,
      DateTimeFormat.forPattern("MM-dd-yyyy HH:mm:ssZ").getParser,
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSS").getParser,
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSZZ").getParser
    )
    val formatter = new DateTimeFormatterBuilder().append(null, parsers).toFormatter()

    def reads(json: JsValue): JsResult[DateTime] =
      json match {
        case JsNumber(num) if num.isValidLong => JsSuccess(new DateTime(num.toLong))
        case JsString(str) =>
          Option(str) match {
            case Some(s) =>
              s forall Character.isDigit match {
                case true => Try(new DateTime(s.toLong)).fold(err => JsError(JsonValidationError(Seq(err.getMessage))), JsSuccess(_))
                case false =>
                  Try(formatter.withOffsetParsed().parseDateTime(s)) match {
                    case Success(dt) =>
                      JsSuccess(dt)
                    case Failure(msg) => JsError(JsonValidationError(Seq(msg.toString)))
                  }
              }
            case None => JsError(s"Invalid value given for dateTime : '${json.toString()}'")
          }
        case _ => JsError("formatted DateTime value expected")
      }
  }

  implicit val dtWrites: Writes[DateTime] = new Writes[org.joda.time.DateTime] {
    // API should return the ISO-8601 format for public consumption
    val df = org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")

    def writes(d: org.joda.time.DateTime): JsValue = JsString(d.toString(df))
  }

  implicit val dtFormat: Format[DateTime] = Format(dtReads, dtWrites)

  //
  // Java Time API - Zoned
  // /////////////////////////////////////
  implicit val zdtReads: Reads[ZonedDateTime] = new Reads[ZonedDateTime] {
    def reads(json: JsValue): JsResult[ZonedDateTime] =
      json match {
        case JsNumber(num) => JsError("Unsupported format")
        case JsString(str) =>
          Option(str) match {
            case Some(s) => JsSuccess(ZonedDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            case None    => JsError(s"Invalid value given for dateTime : '${json.toString()}'")
          }
        case _ => JsError("formatted DateTime value expected")
      }
  }

  implicit val zdtWrites: Writes[ZonedDateTime] = new Writes[ZonedDateTime] {
    def writes(d: ZonedDateTime): JsValue = JsString(d.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  }

  implicit val zdtFormat: Format[ZonedDateTime] = Format(zdtReads, zdtWrites)

  //
  // Scala Duration Format
  // /////////////////////////////////////
  implicit val _DurationReads: Reads[Duration] = new Reads[Duration] {
    def reads(json: JsValue): JsResult[Duration] = json match {
      case JsNumber(s) =>
        Option(s) match {
          case Some(millis) => JsSuccess(FiniteDuration.apply(millis.toLong, TimeUnit.MILLISECONDS).asInstanceOf[Duration])
          case None         => JsError(s"Invalid value given for duration : '${json.toString()}'")
        }
      case _ => JsError("Long value expected")
    }
  }
  implicit val _DurationsWrites: Writes[Duration] = new Writes[Duration] {
    def writes(dur: Duration): JsValue = JsNumber(dur.toMillis)
  }
  implicit val _durationFormat: Format[Duration] = Format(_DurationReads, _DurationsWrites)

  //
  // Scala FiniteDuration
  // /////////////////////////
  implicit val durReads: Reads[FiniteDuration] = new Reads[FiniteDuration] {
    def reads(json: JsValue): JsResult[FiniteDuration] = json match {
      case JsNumber(s) =>
        Option(s) match {
          case Some(s) => JsSuccess(FiniteDuration(s.toLong, TimeUnit.MILLISECONDS))
          case None    => JsError(s"Invalid value given for duration : '${json.toString()}'")
        }
      case _ => JsError("Long value expected")
    }
  }

  implicit val durWrites: Writes[FiniteDuration] = new Writes[FiniteDuration] {
    def writes(dur: FiniteDuration): JsValue = JsNumber(dur.toMillis)
  }

  implicit val durationFormat: Format[FiniteDuration] = Format(durReads, durWrites)

  //
  // Joda Time Period
  // /////////////////////////
  implicit val periodReads: Reads[Period] = new Reads[Period] {
    def reads(json: JsValue): JsResult[Period] = json match {
      case JsString(s) =>
        Option(s) match {
          case Some(s) => JsSuccess(Period.parse(s))
          case None    => JsError(s"Invalid value given for IOS8601 period : '${json.toString()}'")
        }
      case _ => JsError("String value expected for IOS8601 period")
    }
  }

  implicit val periodWrites: Writes[Period] = new Writes[Period] {
    def writes(p: Period): JsValue = JsString(p.toString)
  }

  implicit val periodFormat: Format[Period] = Format(periodReads, periodWrites)

}
