package io.timeli.sk8s

import org.scalatest.{ FunSuite, Matchers }

class EncryptedDataHarness extends FunSuite with Matchers {

  //
  // DO NOT COMMIT ACTUAL USERNAMES AND PASSWORDS HERE
  //

  val template: String =
    """apiVersion: v1
      |kind: Secret
      |metadata:
      |  name: NAME_REPLACE
      |data:""".stripMargin

  test("create secrets yaml") {
    val yamlName = "text-cassandra-encrypted"
    println()
    println(s"cat > $yamlName.yml  <<EOF")
    println(template.replace("NAME_REPLACE", yamlName))
    dumpParams(
      true, //
      "username" -> "username",
      "password" -> "password"
    //
    )
    println("EOF")
  }

  test("test") {
    println(VolumeFiles.encrypt("username"))
    println(VolumeFiles.encrypt("password"))
  }

  def dumpTemplate(name: String, template: String): Unit = println(template.replace("NAME_REPLACE", name))

  def dumpParams(encrypt: Boolean, params: (String, String)*): Unit = {
    params
      .map { case (k, v) => variable(k, v, encrypt = encrypt) }
      .foreach(println)
  }

  def variable(key: String, value: String, encrypt: Boolean = false): String = {
    val valueStr: String = if (encrypt)
      VolumeFiles.encrypt(value).map(VolumeFiles.toBase64).get // for transport in a yaml file
    else
      VolumeFiles.toBase64(value)
    s"  $key: $valueStr"
  }

}
