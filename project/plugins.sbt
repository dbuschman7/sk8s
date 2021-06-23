logLevel := Level.Warn

//
//Build Helpers
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.9")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

//
// Publish
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.7")
//
// Code Management
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")
//