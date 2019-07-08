package me.lightspeed7.sk8s

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.{ AbstractModule, Provider, Provides }
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject

class Sk8sBindings extends AbstractModule {

  def generate(appInfo: AppInfo) =
    Seq( //
      //
      bind(classOf[RunMode]).toInstance(RunMode.currentRunMode),
      bind(classOf[AppInfo]).toInstance(appInfo),
      //
      bind(classOf[Sk8sContext]).toProvider(classOf[Sk8sContextProvider]),
      bind(classOf[BackgroundTasks]).toProvider(classOf[BackgroundTasksProvider])
      //
    )

  override def configure(): Unit = throw new RuntimeException("Use generate(appInfo) instead")
}

class BackgroundTasksProvider @Inject()(implicit appCtx: Sk8sContext) extends Provider[BackgroundTasks] with LazyLogging {

  lazy val bTasks = new BackgroundTasks()

  @Provides
  override def get: BackgroundTasks = bTasks

}

class Sk8sContextProvider @Inject()(implicit appInfo: AppInfo, system: ActorSystem, mat: Materializer)
    extends Provider[Sk8sContext]
    with LazyLogging {

  lazy val ctx = Sk8sContext(appInfo)

  @Provides
  override def get: Sk8sContext = ctx

}
