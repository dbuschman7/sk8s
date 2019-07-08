package me.lightspeed7.sk8s

import com.typesafe.scalalogging.StrictLogging
import me.lightspeed7.sk8s.telemetry.TelemetryRegistry
import me.lightspeed7.sk8s.util.AutoClose
import org.joda.time.DateTime

object Application extends App with StrictLogging {

  for (app <- AutoClose(new BackendApplication(AppInfo(BuildInfo.name, BuildInfo.version, new DateTime(BuildInfo.buildTime))))) {

    import app._

    logger.info(s"Beginning app '${appInfo.appName}'")
    try {
      // Stand up the application internals
      //
      val _       = Variables.source(Sources.env, "MY_POD_IP", Constant("unknown")).value
      val counter = TelemetryRegistry.counter("david")

      // Standup Completed
      logger.info(banner)

      Variables.logConfig(logger.underlying)

      // Add code here

      runUntilStopped() // daemon mode if desired
      //
    } catch {
      case ex: Throwable => shutdown(ex)
    } finally {
      logger.debug("***********************************************************")
      logger.debug("** Shutdown")
      logger.debug("***********************************************************")

    }

  }

  def banner: String = // http://patorjk.com/software/taag/#p=display&f=Big%20Money-sw&t=Type%20App%20Name
    """
      |  ______                       _______                       __                                  __
      | /      \                     /       \                     /  |                                /  |
      |/$$$$$$  |  ______    ______  $$$$$$$  |  ______    _______ $$ |   __   ______   _______    ____$$ |
      |$$ |__$$ | /      \  /      \ $$ |__$$ | /      \  /       |$$ |  /  | /      \ /       \  /    $$ |
      |$$    $$ |/$$$$$$  |/$$$$$$  |$$    $$<  $$$$$$  |/$$$$$$$/ $$ |_/$$/ /$$$$$$  |$$$$$$$  |/$$$$$$$ |
      |$$$$$$$$ |$$ |  $$ |$$ |  $$ |$$$$$$$  | /    $$ |$$ |      $$   $$<  $$    $$ |$$ |  $$ |$$ |  $$ |
      |$$ |  $$ |$$ |__$$ |$$ |__$$ |$$ |__$$ |/$$$$$$$ |$$ \_____ $$$$$$  \ $$$$$$$$/ $$ |  $$ |$$ \__$$ |
      |$$ |  $$ |$$    $$/ $$    $$/ $$    $$/ $$    $$ |$$       |$$ | $$  |$$       |$$ |  $$ |$$    $$ |
      |$$/   $$/ $$$$$$$/  $$$$$$$/  $$$$$$$/   $$$$$$$/  $$$$$$$/ $$/   $$/  $$$$$$$/ $$/   $$/  $$$$$$$/
      |          $$ |      $$ |
      |          $$ |      $$ |
      |          $$/       $$/
      |""".stripMargin
}
