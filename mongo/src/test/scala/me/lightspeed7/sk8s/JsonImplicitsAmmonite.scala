package me.lightspeed7.sk8s

import me.lightspeed7.sk8s.json.JsonImplicits
import os.RelPath
import play.api.libs.json.{ Format, JsError, JsString, JsSuccess, Reads, Writes }

trait JsonImplicitsAmmonite extends JsonImplicits {

  //
  // Ammonite Ops RelPath
  // ////////////////////////////
  implicit val relPathReads: Reads[RelPath] = {
    case json @ JsString(s) =>
      Option(s) match {
        case Some(s) => JsSuccess(RelPath(s))
        case None    => JsError(s"Invalid value given for relPath : '${json.toString()}'")
      }
    case _ => JsError("String value expected")
  }

  implicit val relPathWrites: Writes[RelPath] = (path: RelPath) => JsString(path.toString())

  implicit val relPathFormat: Format[RelPath] = Format(relPathReads, relPathWrites)

  //
  // Ammonite Ops Path
  //////////////////////////////
  implicit val aPathReads: Reads[os.Path] = {
    case json @ JsString(str) =>
      Option(str) match {
        case Some(s) =>
          JsSuccess(os.Path(s))
        case None => JsError(s"Invalid value given for date : '${json.toString()}'")
      }
    case _ => JsError("formatted date value expected")
  }

  implicit val aPathWrites: Writes[os.Path] = (p: os.Path) => JsString(p.toIO.getAbsolutePath)

  implicit val aPathFormat: Format[os.Path] = Format(aPathReads, aPathWrites)

}
