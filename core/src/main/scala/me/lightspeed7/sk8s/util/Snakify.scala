package me.lightspeed7.sk8s.util

object Snakify {

  implicit class Snakify(in: String) {
    def snakify: String =
      in.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2").replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase

    def snakeClassname: String = in.replaceAll("\\.", "_").toLowerCase
  }

}
