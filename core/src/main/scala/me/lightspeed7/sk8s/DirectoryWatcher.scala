package me.lightspeed7.sk8s

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute._

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap

//
// Original source code : https://gist.github.com/eberle1080/1241375
//
// TODO: CLean this up and make is more scala centric, only made a quick pass
// ////////////////////////////////////////////////////////////////////////////////////
class DirectoryWatcher(callBack: Seq[Path] => Unit, debug: Boolean = false) extends Runnable {

  private case class Watch(key: WatchKey, path: Path, recursive: Boolean = false)

  private val watchService: WatchService     = FileSystems.getDefault.newWatchService()
  private val keys: TrieMap[WatchKey, Watch] = TrieMap.empty

  final def trace(msg: => String): Unit =
    if (debug) println(msg)

  final private def printEvent(event: WatchEvent[_], watch: Watch): Unit = {
    val event_path = event.context().asInstanceOf[Path]
    val dir        = watch.path

    import StandardWatchEventKinds._
    event.kind match {
      case ENTRY_CREATE => trace(s"Entry created:  $event_path - $dir")
      case ENTRY_DELETE => trace(s"Entry deleted:  $event_path - $dir")
      case ENTRY_MODIFY => trace(s"Entry modified: $event_path - $dir")
      case unknown      => trace(s"Entry unknown($unknown): $event_path - $dir")
    }
  }

  final private def register(dir: Path, recursive: Boolean): Unit = {
    val key =
      dir.register(watchService,
                   StandardWatchEventKinds.ENTRY_CREATE,
                   StandardWatchEventKinds.ENTRY_MODIFY,
                   StandardWatchEventKinds.ENTRY_DELETE
      )

    if (debug) {
      keys.get(key) match {
        case None                            => trace(s"register: $dir")
        case Some(prev) if !dir.equals(prev) => trace(s"update: $prev  -> $dir")
        case Some(prev)                      => trace(s"duplicate - $prev")
      }
    }

    keys(key) = Watch(key, dir, recursive)
  }

  /**
    * Register a particular file or directory to be watched
    */
  def register(dir: Path): Unit = register(dir, recursive = false)

  /**
    * Recursively register directories
    */
  def registerAll(start: Path): Unit = {

    implicit def makeDirVisitor(f: Path => Unit): SimpleFileVisitor[Path] =
      new SimpleFileVisitor[Path] {
        override def preVisitDirectory(p: Path, attrs: BasicFileAttributes): FileVisitResult = {
          f(p)
          FileVisitResult.CONTINUE
        }
      }

    trace("Scanning " + start + "...")
    Files.walkFileTree(start,
                       (f: Path) => {
                         register(f, recursive = true)
                       }
    )
    trace("Done.")
  }

  private def registerNewChildren(child: Path): Path =
    try {
      if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
        registerAll(child)
      }
      child
    } catch {
      case ioe: IOException =>
        println("IOException: " + ioe)
        throw ioe
      case e: Exception =>
        println("Exception: " + e)
        throw e
    }

  private def logEvent[T](event: WatchEvent[T], watch: Watch): Path = {
    val name  = event.context().asInstanceOf[Path]
    val child = watch.path.resolve(name)
    if (debug) printEvent(event, watch)
    child
  }

  def stop(path: Path): Unit =
    keys
      .filter(_._2.path == path)
      .foreach { case (key, _) => keys.remove(key) }

  /**
    * The main directory watching thread
    */
  override def run(): Unit =
    try {
      //      if (recursive) registerAll(path) else register(path)

      while (true) {
        val key = watchService.take()
        val collectedEvents: Option[Seq[Path]] = keys.get(key).map { watch: Watch =>
          key.pollEvents().asScala.flatMap { event =>
            val kind = event.kind
            import StandardWatchEventKinds._
            kind match {
              case OVERFLOW                        => None // ignore
              case ENTRY_CREATE if watch.recursive => Option(registerNewChildren(logEvent(event, watch)))
              case _                               => Option(logEvent(event, watch))
            }
          }
        }

        collectedEvents.foreach(callBack) // inform the invoker

        if (!key.reset()) {
          keys.remove(key)
          if (keys.isEmpty) {
            println("keys empty")
          }
        }
      }
    } catch {
      case ie: InterruptedException => println("InterruptedException: " + ie)
      case ioe: IOException         => println("IOException: " + ioe)
      case e: Exception             => println("Exception: " + e)
    }
}
