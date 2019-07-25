package me.lightspeed7.sk8s

import com.google.inject.AbstractModule
import com.typesafe.scalalogging.LazyLogging

class Sk8sBindings extends AbstractModule with LazyLogging {

  def generate(appInfo: AppInfo) = {

    implicit val ctx: Sk8sContext = Sk8sContext.create(appInfo)
    lazy val bTasks = new BackgroundTasks()

    Seq( //
      //
      bind(classOf[RunMode]).toInstance(RunMode.currentRunMode),
      bind(classOf[AppInfo]).toInstance(appInfo),
      //
      bind(classOf[Sk8sContext]).toInstance(ctx),
      bind(classOf[BackgroundTasks]).toInstance(bTasks)
      //
    )
  }

  override def configure(): Unit = throw new RuntimeException("Use generate(appInfo) instead")
}

