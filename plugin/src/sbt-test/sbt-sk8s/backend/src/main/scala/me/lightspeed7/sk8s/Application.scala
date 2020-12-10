package me.lightspeed7.sk8s

import java.time.ZonedDateTime

object PluginApplication extends App {
  import util._

  for (app <- AutoClose(new BackendApplication(sk8s.build.appInfo))) {
    import app._
    println("Sk8s - " + appInfo.toString)
  }
}
