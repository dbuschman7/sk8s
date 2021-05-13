package me.lightspeed7.sk8s

import java.nio.file.{ Path, Paths }

import org.apache.ivy.core.settings._
import org.apache.ivy.plugins.parser.m2._
import org.apache.ivy.plugins.parser.xml._

object Ivy2Pom extends App {

  def convert(in: Path, out: Path): Unit =
    try {
      System.setOut(System.err)
      val parser = XmlModuleDescriptorParser.getInstance
      val md     = parser.parseDescriptor(new IvySettings, in.toFile.toURI.toURL, false)
      PomModuleDescriptorWriter.write(md, out.toFile, new PomWriterOptions)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        System.exit(1)
    }

  val baseDir = Paths.get("/Users/dave/dev/dbuschman7/migrate/sbt-plugins/sk8s/")

  val versions = Seq("0.6.0", "0.6.1", "0.6.3", "0.6.4", "0.6.5", "0.6.7", "0.6.8", "0.7.0", "0.7.1")

  versions.map { v =>
    val ivy = Paths.get(s"$baseDir/$v/ivy.xml")
    val pom = Paths.get(s"$baseDir/$v/pom.xml")

    println(s"File $ivy - exists - ${ivy.toFile.exists()}")
    convert(ivy, pom)
    println(s"File $pom - exists - ${pom.toFile.exists()}")
  }

}
