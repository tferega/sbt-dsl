scalaVersion := "2.11.8"

enablePlugins(SbtDslPlugin)
dslTargets += Options.Target.RevenjJava

libraryDependencies += "org.revenj" % "revenj-core" % "0.9.4"
