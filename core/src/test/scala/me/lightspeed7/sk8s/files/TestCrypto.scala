package me.lightspeed7.sk8s.files

import javax.crypto.Cipher
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }

object TestCrypto {

  val data: String =
    "WHOO-HOO! I'M RIGHT HERE! I'M RIGHT HERE! YOU WANT SOME O' ME?! YEAH YOU DO! COME ON! COME ON! AAAAAH! Whoo-hoo!"

  implicit val crypto: Sk8sCrypto = new Sk8sCrypto {

    // Firefly theme
    private val iv                = new IvParameterSpec("First rule of battle, little one ... don't ever let them know where you are.".substring(0, 16).getBytes)
    private val rawAESKey: String = "Course, there're other schools of thought.".substring(0, 16)
    private val secretKey         = new SecretKeySpec(rawAESKey.getBytes, "AES")

    override def cipher(mode: Int): Cipher = {
      val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      cipher.init(mode, secretKey, iv)
      cipher
    }

    override def padding: String = rawAESKey
  }
}
