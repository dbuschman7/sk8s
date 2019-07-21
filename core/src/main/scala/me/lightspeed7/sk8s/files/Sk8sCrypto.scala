package me.lightspeed7.sk8s.files

import java.util.Base64

import com.typesafe.scalalogging.LazyLogging
import javax.crypto.{ Cipher => JCipher }

import scala.util.{ Failure, Success, Try }

trait Sk8sCrypto extends LazyLogging {

  // abstract methods
  protected def cipher(mode: Int): JCipher
  def padding: String

  // impl
  import me.lightspeed7.sk8s.util.String._

  private val enc = Base64.getEncoder
  private val dec = Base64.getDecoder

  def toBase64(in: String): String = enc.encodeToString(in.getBytes())

  def fromBase64(in: String): Option[String] = Try(new String(dec.decode(in))).toOption

  def encrypt(raw: String): Option[String] =
    raw.notBlank
      .flatMap { r =>
        Try(enc.encodeToString(cipher(JCipher.ENCRYPT_MODE).doFinal(pad(r).getBytes))).toOption
      }

  def decrypt(raw: String): Option[String] =
    raw.notBlank
      .flatMap { r =>
        Try(unpad(new String(cipher(JCipher.DECRYPT_MODE).doFinal(dec.decode(r))))) match {
          case Success(value) => Some(value)
          case Failure(ex) =>
            logger.error("Unable to decrypted secret", ex)
            None
        }
      }
  private def pad(input: String): String = {
    val temp = input + "|" + padding
    temp.substring(0, (temp.length() / 16) * 16)
  }

  private def unpad(input: String): String = input.substring(0, input.lastIndexOf("|"))

}
