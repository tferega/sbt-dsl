scalaVersion := "2.11.8"

enablePlugins(SbtDslPlugin)
dslTargets += Target.RevenjJava

libraryDependencies += "org.revenj" % "revenj-core" % "0.9.4"
