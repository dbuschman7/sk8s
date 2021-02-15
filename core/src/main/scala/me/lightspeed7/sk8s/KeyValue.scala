package me.lightspeed7.sk8s

final case class KeyValue(key: String, value: String = "") {
  def yaml: String = key + ": " + value
  def json: String = s""""$key" : "$value""""
}
