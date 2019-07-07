package me.lightspeed7.sk8s

import java.io.{ File, PrintWriter }
import java.nio.file.{ Path, Paths }

import me.lightspeed7.sk8s.util.AutoClose

object WatchApp extends App {

  // setup
  // ////////
  val pwd: Path           = Paths.get("sk8s").toAbsolutePath
  val resourcesPath: Path = Paths.get(pwd.toString + "/target")
  val configMap: Path     = Paths.get(resourcesPath.toString, "configMap")
  val testFile: Path      = Paths.get(configMap.toString, "key.name")
  val testFile2: Path     = Paths.get(configMap.toString, "key.name2")

  def writeValue(file: File, value: String): Unit = {
    println(s"Writing $value to $file")
    Thread.sleep(1000)
    for (writer <- AutoClose(new PrintWriter(file))) {
      writer.print(value)
    }
  }

  testFile.toFile.mkdirs()
  testFile.toFile.delete()
  testFile2.toFile.delete()

  // run
  // ////////

  val dirWatcher = new DirectoryWatcher({ in =>
    in.foreach(println)
  }, true)
  val watchThread = new Thread(dirWatcher)
  watchThread.setDaemon(true)
  watchThread.setName("WatcherService")
  watchThread.start()

  dirWatcher.registerAll(configMap)

  (1 to 4).foreach { i =>
    writeValue(testFile.toFile, "value" + i)
    if (i % 2 == 0) {
      writeValue(testFile2.toFile, "value" + i)
    }
    Thread.sleep(10 * 1000)
  }

  // stop watching
  println(s"stopping watch - $configMap")
  dirWatcher.stop(configMap)
  writeValue(testFile.toFile, "value")
  Thread.sleep(10 * 1000)

  watchThread.interrupt()
  println("DONE")
}
