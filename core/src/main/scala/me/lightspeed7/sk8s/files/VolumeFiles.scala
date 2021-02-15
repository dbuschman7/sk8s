package me.lightspeed7.sk8s.files

import java.nio.file.{Path, Paths}

//
//  Mounted Files
// ////////////////////////////
final case class VolumeFiles(name: String, mountPath: Path, encrypted: Boolean) {

  val fullPath: Path = Paths.get(mountPath.toString, name)

  def value(key: String)(implicit crypto: Sk8sCrypto): Option[String] =
    Sk8sFileIO
      .getContents(fullPath, key)
      .flatMap { raw =>
        if (encrypted) crypto.decrypt(raw) else Some(raw)
      }
}
