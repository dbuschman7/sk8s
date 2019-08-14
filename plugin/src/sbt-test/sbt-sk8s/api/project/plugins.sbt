sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("me.lightspeed7" % "sbt-sk8s" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.7")