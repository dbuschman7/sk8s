package me.lightspeed7.sk8s

import java.nio.file.{ Path, Paths }

import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.files.{ Sk8sCrypto, VolumeFiles }
import org.slf4j.Logger

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }

//
// Configuration Variables
// ///////////////////////////////
trait Variable[T] extends LazyLogging {
  def name: String

  def displayName: String

  def value: T

  def exists: Boolean

  def isConstant: Boolean = false

  def security: Boolean = false

  def valueStr: String = if (security && RunMode.currentRunMode.requiresSecurity) "**********" else value.toString

  override def toString: String = dump(2)

  def dump(indent: Int): String = {
    val value: String = Try(valueStr) match {
      case Success(v)                        => v
      case Failure(_: IllegalStateException) => ""
      case Failure(ex)                       => throw ex;
    }
    pad(indent) + f"$displayName%-60s ($exists%5s)    : $value%-40s"
  }

  def pad(indent: Int): String = {
    import me.lightspeed7.sk8s.util.String._
    "".pad.left(' ', indent)
  }

  protected def convert[TV](value: String)(implicit ct: ClassTag[TV]): Option[TV] =
    ct.runtimeClass.getSimpleName.toLowerCase match {
      case "string"  => Option(value.asInstanceOf[TV])
      case "boolean" => Option(value.toBoolean.asInstanceOf[TV])
      case "int"     => Option(value.toInt.asInstanceOf[TV])
      case "long"    => Option(value.toLong.asInstanceOf[TV])
      case "duration" =>
        val p = Try(value.trim.toLong) match {
          case Success(lng) => Some(Duration(lng, _root_.java.util.concurrent.TimeUnit.MILLISECONDS))
          case Failure(_)   => Try(Duration(value.trim)).toOption // string variant
        }
        p.map(_.asInstanceOf[TV])
      case "finiteduration" =>
        val p = Try(value.trim.toLong) match {
          case Success(lng) => Some(FiniteDuration(lng, _root_.java.util.concurrent.TimeUnit.MILLISECONDS))
          case Failure(_)   => Try(Duration(value.trim)).toOption // string variant
        }
        p.map(_.asInstanceOf[TV])
      case unknown => throw new IllegalArgumentException(s"Data type not defined for '$unknown'")
    }

  def register(): Variable[T] = {
    Variables.registered.put(name, this)
    this
  }

}

/**
 * Holder for a constant value, use to handling defaults
 */
sealed case class Constant[T](name: String, constantValue: T, override val security: Boolean = false) extends Variable[T] {

  val displayName: String = "Constant - " + name

  def value: T = constantValue

  def exists: Boolean = true

  override def isConstant: Boolean = true
}

object Constant {
  def apply[T](constantValue: T): Constant[T] = apply(constantValue, security = false)

  def apply[T](constantValue: T, security: Boolean): Constant[T] =
    Constant(constantValue.toString, constantValue, security)
}

/**
 * Returns data based on which RunMode is activated, otherwise the default
 */
sealed case class RunModeVariable[T](name: String,
                                     default: Constant[T],
                                     runModeMap: Map[RunMode, Variable[T]],
                                     override val security: Boolean = false)(implicit val ct: ClassTag[T])
    extends Variable[T] {

  val displayName: String = name

  def value: T = runModeVar.value

  def exists: Boolean = runModeMap.get(RunMode.currentRunMode).isDefined

  private[sk8s] def runModeVar = runModeMap.getOrElse(RunMode.currentRunMode, default)
}

/**
 * Returns a value from a config source, possibly dynamic
 */
sealed case class SourceVariable[T](name: String, source: Source, default: Variable[T], override val security: Boolean = false)(
    implicit val ct: ClassTag[T])
    extends Variable[T] {

  val displayName: String = s"Src(${source.name()}) - " + name

  def value: T =
    source.value(name).flatMap { in: String =>
      convert(in)
    } getOrElse default.value

  def exists: Boolean = source.value(name).isDefined
}

/**
 * allows a list of variables where the first one with a value is returned
 */
sealed case class FirstValueVariable[T](name: String, default: Variable[T], ordered: Seq[Variable[T]]) extends Variable[T] {

  def displayName: String = s"Multi($name)"

  override def security: Boolean = found.security // delegate

  // find the first variable that has an existing value
  def found: Variable[T] = ordered.find(_.exists).getOrElse(default)

  def value: T = found.value // delegate

  def exists: Boolean = found.exists // delegate
}

sealed case class ExternalVariable[T](name: String, override val security: Boolean, default: Constant[T], function: () => Option[String])(
    implicit val ct: ClassTag[T])
    extends Variable[T] {

  val displayName: String = "External - " + name

  def exists: Boolean = true // do not know if it exists so we must test for a value

  def value: T =
    function()
      .flatMap { in: String =>
        convert(in)
      }
      .getOrElse(default.value)

  override def valueStr: String = "function()"
}

sealed case class K8sSecretVariable(secret: String, key: String, default: String, mountPath: Path)(implicit crypto: Sk8sCrypto)
    extends Variable[String] {

  lazy val name: String = secret + "." + key

  lazy val volFiles: VolumeFiles = Sk8s.secrets(secret, encrypted = true, mountPath)

  lazy val displayName: String = s"""K8s[Secret]($name)"""

  override val security: Boolean = true // always secured

  def value: String = volFiles.value(key).getOrElse(default)

  def exists: Boolean = volFiles.value(key).isDefined
}

object Variables {
  private[sk8s] val registered: TrieMap[String, Variable[_]] = TrieMap[String, Variable[_]]()

  def terminal[T](varName: String): Variable[T] = new Variable[T] {
    def exists: Boolean = true

    def name: String = varName

    def displayName: String = "Terminal - " + varName

    def value: T = throw new IllegalStateException("Undefined Variable  - " + varName)

    override def valueStr: String = "Undefined"
  }

  def constant[T](name: String, value: T, security: Boolean = false)(implicit ct: ClassTag[T]): Variable[T] =
    new Constant[T](name, value, security).register()

  /**
   * Create a Source based variable
   */
  def source[T](source: Source, name: String, default: Variable[T], security: Boolean = false)(implicit ct: ClassTag[T]): Variable[T] =
    new SourceVariable[T](name, source, default, security).register()

  def maybeSource[T](source: Source, name: String, security: Boolean = false)(implicit ct: ClassTag[T]): Variable[T] =
    new SourceVariable[T](name, source, terminal(name), security).register()

  /**
   * Create a Multi-Value Source Variable
   */
  def multiSource[T](name: String, default: Constant[T], sourceMap: (Source, String, Boolean)*)(implicit ct: ClassTag[T]): Variable[T] = {
    val ordered: Seq[Variable[T]] = sourceMap.map {
      case (src, lookupName, security) => SourceVariable[T](lookupName, src, default, security)
    }
    firstValue(name, ordered :+ default: _*) // already registered
  }

  /**
   * Create a multi-possibility variable
   */
  def firstValue[T](name: String, variables: Variable[T]*)(implicit ct: ClassTag[T]): Variable[T] =
    FirstValueVariable[T](name, terminal(name), variables).register()

  /**
   * Create a RunMode based variable
   */
  def runMode[T](name: String, default: T, runModes: (RunMode, Variable[T])*)(implicit ct: ClassTag[T]): Variable[T] = {
    val secured: Boolean = runModes.foldLeft(false) { (res, next) =>
      res || next._2.security
    }
    RunModeVariable(name, Constant(default, secured), runModes.toMap, secured).register()
  }

  /**
   * Create a variable from a K8s secret
   */
  def secret(secret: String, key: String, default: String, mountPath: Path = Paths.get("/etc"))(implicit crypto: Sk8sCrypto): Variable[String] =
    K8sSecretVariable(secret, key, default, mountPath).register()

  /**
   * Create a External function based variable
   */
  def external[T](name: String, security: Boolean, default: Constant[T], function: () => Option[String])(implicit ct: ClassTag[T]): Variable[T] =
    ExternalVariable(name, security, default, function).register()

  def visitDefinedVariables[T](f: Variable[_] => T): Unit = registered.toSeq.sortBy(m => m._1).map(_._2).map(f)

  def dumpConfiguration(writer: String => Unit): Unit = {
    writer("**")
    writer("** Configuration Variables -- Run Mode = " + RunMode.currentRunMode)
    writer("*" * (60 + 15))
    visitDefinedVariables { v =>
      writer(v.dump(2))
    }
    writer("*" * (60 + 15))
  }

  def logConfig(logger: Logger): Unit = {
    val buf = new StringBuilder("\n")
    dumpConfiguration({ in: String =>
      buf.append(in).append("\n")
    })
    logger.info(buf.toString)
  }
}
