package me.lightspeed7.sk8s.files

import java.nio.file.{ Path, Paths }

import me.lightspeed7.sk8s.util.FileUtils

//
//  Mounted Files
// ////////////////////////////
final case class VolumeFiles(name: String, mountPath: Path, encrypted: Boolean) extends FileUtils {

  val fullPath: Path = Paths.get(mountPath.toString, name)

  def value(key: String)(implicit crypto: Sk8sCrypto): Option[String] =
    getContents(fullPath, key)
      .flatMap { raw =>
        if (encrypted) crypto.decrypt(raw) else Some(raw)
      }
}
