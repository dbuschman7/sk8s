package me.lightspeed7.sk8s

import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap

//
// Config Sources
// ////////////////////////////////
trait Source {
  def name(): String
  def value(name: String): Option[String]
}

sealed case class EnvironmentSource() extends Source {
  lazy val valuesMap: TrieMap[String, String] = {
    val m = new TrieMap[String, String]()
    sys.env.map { case (k, v) => m.put(k, v) }
    m
  }

  private def logIt(msg: String): Unit = {
    println(msg) // println intentional here
    LoggerFactory.getLogger("me.lightspeed7.sk8s").warn(msg)
  }

  def clearVariable(key: String): Option[String] = {
    logIt(s"WARNING WARNING -- Danger Will Robinson - Env Var '$key' has been cleared")
    valuesMap.remove(key)
  }
  def overrideVariable(key: String, value: String): Unit = {
    valuesMap.put(key, value)
    logIt(s"WARNING WARNING -- Danger Will Robinson - Env Var '$key' is now set to '$value'")
  }

  def name(): String                      = "Env"
  def value(name: String): Option[String] = Option(name).flatMap(f => valuesMap.get(f))
}
//
//
object EnvironmentSource {
  lazy val valuesMap: TrieMap[String, String] = {
    val m = new TrieMap[String, String]()
    sys.env.map { case (k, v) => m.put(k, v) }
    m
  }

  private def logIt(msg: String): Unit = {
    println(msg) // println intentional here
    LoggerFactory.getLogger("me.lightspeed7.sk8s").warn(msg)
  }

  def clearVariable(key: String): Option[String] = {
    logIt(s"WARNING WARNING -- Danger Will Robinson - Env Var '$key' has been cleared")
    valuesMap.remove(key)
  }

  def overrideVariable(key: String, value: String): Unit = {
    valuesMap.put(key, value)
    logIt(s"WARNING WARNING -- Danger Will Robinson - Env Var '$key' is now set to '$value'")
  }

  // string
  def value(name: String): Option[String] = Option(name).flatMap(f => valuesMap.get(f))

  def value(name: String, `default`: String): String = value(name).getOrElse(`default`)

  // int
  def valueInt(name: String): Option[Int] = value(name).map(_.toInt)

  def valueInt(name: String, `default`: Int): Int = valueInt(name).getOrElse(`default`)

}

sealed case class SysPropsSource() extends Source {
  def name(): String = "SysProp"
  def value(name: String): Option[String] = Option(name).flatMap { n =>
    Option(System.getProperty(n))
  }
}

case class PropertiesSource(props: _root_.java.util.Properties) extends Source {
  def name(): String = "Props"
  def value(name: String): Option[String] = Option(name).flatMap { n =>
    Option(props.getProperty(n))
  }
}

object Sources {

  lazy val env: Source      = registerSource("env", EnvironmentSource())
  lazy val sysProps: Source = registerSource("sysProps", SysPropsSource())

  private lazy val sources: TrieMap[String, Source] = new TrieMap[String, Source]()

  def getSource(name: String): Source = sources.getOrElse(name, env) // HACK for legacy system

  def registerSource(name: String, source: Source): Source = { sources.put(name, source); source }
  def registerProperties(name: String, properties: _root_.java.util.Properties): Source =
    registerSource(name, PropertiesSource(properties))
}
