package me.lightspeed7.sk8s.server

import me.lightspeed7.sk8s.{RunMode, Sk8sContext, Variables}
import me.lightspeed7.sk8s.services.SystemProperties
import play.api.libs.json.{JsValue, Json}

object JsonConfig {

  def generate(implicit ctx: Sk8sContext): JsValue = {

    import ctx.appInfo

    val buf = new StringBuilder("\n")
    buf.append(s"""{""").append("\n")

    //
    // general
    // /////////////////////
    buf.append(s"""  "runMode" : "${RunMode.currentRunMode}", """).append("\n")
    buf.append(s"""  "appInfo" : ${appInfo.toJson.toString()}, """).append("\n")
    buf.append(s"""  "java"    : "${SystemProperties.javaVersion}", """).append("\n")
    buf.append(s"""  "memory"  : ${MemoryInfo.create().toJson.toString}, """).append("\n")

    //
    // variables
    // /////////////////////
    Variables.dumpJson { in: String =>
      buf.append(in).append("\n")
    }(ctx.appInfo)

    buf.append(s"""}""")

    //
    // parse it
    // //////////////////////
    Json.parse(buf.toString())
  }

}
