package io.timeli.sk8s.util

import com.typesafe.scalalogging.LazyLogging
import io.timeli.sk8s.util.Closeables.logger

class AutoClose[A <: java.lang.AutoCloseable](protected val c: A) {

  // make sure the resource is closed
  def map[B](f: (A) => B): B = try {
    f(c)
  }
  finally {
    c.close()
  }

  def foreach[B](f: (A) => B): B = map(f)

  // Not a proper flatMap, bit it works in for -> yield
  def flatMap[B](f: (A) => B): B = map(f)

}

object AutoClose {
  def apply[A <: java.lang.AutoCloseable](c: A) = new AutoClose(c)
}

// JVM singleton
object Closeables extends LazyLogging with AutoCloseable {

  //
  // Closeable resource handling
  // ////////////////////////////////////
  private final case class Closeable(label: String, closeable: AutoCloseable)

  private var closeables: Seq[Closeable] = Seq.empty

  def registerCloseable[T <: AutoCloseable](label: String, closeable: => T): T = synchronized {
    logger.info(s"Register Closeable - $label")
    val temp = closeables :+ Closeable(label, closeable)
    closeables = temp
    closeable
  }

  def close(): Unit = synchronized {
    logger.info("Closing AutoCloseables ...")
    closeables
      .reverse // opposite of registration
      .foreach {
        case Closeable(l, c) =>
          logger.info(s"Closing - $l")
          c.close()
      }
  }

}
