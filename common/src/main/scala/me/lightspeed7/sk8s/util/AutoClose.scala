package me.lightspeed7.sk8s.util

class AutoClose[A <: java.lang.AutoCloseable](protected val c: A) {

  // make sure the resource is closed
  def map[B](f: A => B): B =
    try {
      f(c)
    } finally {
      c.close()
    }

  def foreach[B](f: A => B): B = map(f)

  // Not a proper flatMap, bit it works in for -> yield
  def flatMap[B](f: A => B): B = map(f)

}

object AutoClose {
  def apply[A <: java.lang.AutoCloseable](c: A) = new AutoClose(c)
}
