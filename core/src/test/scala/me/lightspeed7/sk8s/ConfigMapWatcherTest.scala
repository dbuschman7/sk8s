package me.lightspeed7.sk8s

import java.io.{File, PrintWriter}
import java.nio.file.{Path, Paths}

import me.lightspeed7.sk8s.util.AutoClose

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.util.Success
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ConfigMapWatcherTest extends AnyFunSuite with Matchers {

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

  test("DirectoryWatcher") {

    testFile.toFile.mkdirs()
    testFile.toFile.delete()
    testFile2.toFile.delete()

    val p = Promise[Boolean]()
    val f = p.future

    writeValue(testFile.toFile, "value")

    val dirWatcher = new DirectoryWatcher({ _ =>
                                            p.complete(Success(true))
                                          },
                                          true
    )
    val watchThread = new Thread(dirWatcher)
    watchThread.setDaemon(true)
    watchThread.setName("WatcherService")
    watchThread.start()

    dirWatcher.registerAll(configMap)

    writeValue(testFile.toFile, "value1")

    Await.result(f, 20 seconds) should be(true)

    dirWatcher.stop(configMap)
  }

}
