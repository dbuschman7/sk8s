package me.lightspeed7.sk8s


import java.nio.file.{Path, Paths}
import java.util.Base64

import com.typesafe.scalalogging.LazyLogging
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import javax.crypto.{Cipher => JCipher}
import me.lightspeed7.sk8s.util.FileUtils

import scala.util.{Failure, Success, Try}

//
//  Mounted Files
// ////////////////////////////
final case class VolumeFiles(name: String, mountPath: Path, encrypted: Boolean) extends FileUtils {

  val fullPath: Path = Paths.get(mountPath.toString, name)

  def value(key: String): Option[String] = {
    getContents(fullPath, key)
      .flatMap { raw =>
        if (encrypted) VolumeFiles.decrypt(raw) else Some(raw)
      }
  }
}

object VolumeFiles extends LazyLogging {

  implicit class StringPimps(s: String) {

    def notEmpty: Option[String] = s match {
      case "" => None
      case _  => Option(s)
    }

    def notBlank: Option[String] = s.notEmpty.flatMap(_ => s.trim.notEmpty)

  }

  // Firefly theme
  private val iv = new IvParameterSpec("No power in the 'verse can stop me.".substring(0, 16).getBytes)
  private val rawAESKey: String = "Also, I can kill you with my brain.".substring(0, 16)
  private val secretKey = new SecretKeySpec(rawAESKey.getBytes, "AES")
  private val enc = Base64.getEncoder
  private val dec = Base64.getDecoder

  private def cipher(mode: Int): JCipher = {
    val cipher = JCipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(mode, secretKey, iv)
    cipher
  }

  def toBase64(in: String): String = enc.encodeToString(in.getBytes())

  def fromBase64(in: String): Option[String] = Try(new String(dec.decode(in))).toOption

  def encrypt(raw: String): Option[String] = {
    raw.notBlank
      .flatMap { r => Try(enc.encodeToString(cipher(JCipher.ENCRYPT_MODE).doFinal(pad(r).getBytes))).toOption }
  }

  def decrypt(raw: String): Option[String] = {
    raw.notBlank
      .flatMap { r =>
        Try(unpad(new String(cipher(JCipher.DECRYPT_MODE).doFinal(dec.decode(r))))) match {
          case Success(value) => Some(value)
          case Failure(ex) =>
            logger.error("Unable to decrypted secret", ex)
            None
        }
      }
  }

  private def pad(input: String): String = {
    val temp = input + "|" + rawAESKey
    temp.substring(0, (temp.length() / 16) * 16)
  }

  private def unpad(input: String): String = input.substring(0, input.lastIndexOf("|"))

}