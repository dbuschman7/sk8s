package me.lightspeed7.sk8s

import java.time.ZonedDateTime

object Application extends App {
  import util._

  for (app <- AutoClose(new BackendApplication(sk8s.build.appInfo))) {
    import app._
    println("Sl8s - " + appInfo.toString)
  }
}
