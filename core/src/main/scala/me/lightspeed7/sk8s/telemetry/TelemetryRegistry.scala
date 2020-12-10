package me.lightspeed7.sk8s.telemetry

import me.lightspeed7.sk8s.AppInfo
import org.joda.time.DateTime

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

//
// Telemetry Types
// ////////////////////////////////////
trait Telemetry {

  implicit class Snakify(in: String) {
    def snakify: String =
      in.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2").replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase

    def snakeClassname: String = in.replaceAll("\\.", "_").toLowerCase
  }

  def getType: String // define this in sub classes

  def toMetricName(name: String, appInfo: AppInfo): String =
    "sk8s_" + appInfo.appName.snakify + "_" + name.snakify.replace("-", "_")
}

trait TimerLike {
  def time[A](f: => A): A

  def time[A](f: => Future[A])(implicit ec: ExecutionContext): Future[A]

  def update(latencyInSeconds: Double): Unit

  def update(latencyInMillis: Long): Unit

  def update(latency: Duration): Unit

  def deltaFrom(startTime: DateTime): Long

  def deltaFrom(startTimeMillis: Long): Long
}
