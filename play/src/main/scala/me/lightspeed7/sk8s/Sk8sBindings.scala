package me.lightspeed7.sk8s

import com.google.inject.AbstractModule

class Sk8sBindings extends AbstractModule {

  def generate(appInfo: AppInfo, ctx: Sk8sContext) =
    Seq( //
      //
      bind(classOf[RunMode]).toInstance(RunMode.currentRunMode),
      bind(classOf[AppInfo]).toInstance(appInfo),
      //
      bind(classOf[Sk8sContext]).toInstance(ctx),
      bind(classOf[BackgroundTasks]) asEagerSingleton ()
      //
    )

  override def configure(): Unit = throw new RuntimeException("Use generate(appInfo) instead")
}
