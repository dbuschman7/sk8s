package me.lightspeed7.sk8s

import java.io.File
import java.nio.file.{ Path, Paths }
import java.time.ZonedDateTime

import javax.inject.Inject
import me.lightspeed7.sk8s.logging.LazyJsonLogging
import play.api._
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

class GlobalModule extends Sk8sBindings {

  implicit val appInfo: AppInfo = AppInfo(BuildInfo.name, BuildInfo.version, ZonedDateTime.now())

  override def configure(): Unit = {
    generate(appInfo)
    //
    bind(classOf[Initialize]).asEagerSingleton() // initialize actors
  }

}

class Initialize @Inject()( //
                           /*    */ val lifecycle: ApplicationLifecycle,
                           implicit val appCtx: Sk8sContext)
    extends LazyJsonLogging {

  def isKubernetes(basePath: Path = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token")): Boolean = new File(basePath.toString).exists()

  if (isKubernetes()) {
    logger.info(s"Kubernetes - ${Sk8s.serviceAccount().isKubernetes}")

  } else {
    logger.warn("Kubernetes NOT detected !!! ")
  }

  lifecycle.addStopHook { () =>
    import appCtx.ec
    Future {
      // add shutdown code here
    }
  }

  // stand up objects here

  // finish
  //
  // Go to : http://patorjk.com/software/taag/#p=display&f=Big%20Money-sw&t=Type%20App%20Name
  logger.info("""
      |  ______                       _______             __        __  __
      | /      \                     /       \           /  |      /  |/  |
      |/$$$$$$  |  ______    ______  $$$$$$$  | __    __ $$ |____  $$ |$$/   _______
      |$$ |__$$ | /      \  /      \ $$ |__$$ |/  |  /  |$$      \ $$ |/  | /       |
      |$$    $$ |/$$$$$$  |/$$$$$$  |$$    $$/ $$ |  $$ |$$$$$$$  |$$ |$$ |/$$$$$$$/
      |$$$$$$$$ |$$ |  $$ |$$ |  $$ |$$$$$$$/  $$ |  $$ |$$ |  $$ |$$ |$$ |$$ |
      |$$ |  $$ |$$ |__$$ |$$ |__$$ |$$ |      $$ \__$$ |$$ |__$$ |$$ |$$ |$$ \_____
      |$$ |  $$ |$$    $$/ $$    $$/ $$ |      $$    $$/ $$    $$/ $$ |$$ |$$       |
      |$$/   $$/ $$$$$$$/  $$$$$$$/  $$/        $$$$$$/  $$$$$$$/  $$/ $$/  $$$$$$$/
      |          $$ |      $$ |
      |          $$ |      $$ |
      |          $$/       $$/
    """.stripMargin)

  Variables.logConfig(logger.underlying.underlying)

}
