scalaVersion := "2.11.8"

enablePlugins(SbtDslPlugin)
dslTargets += com.dslplatform.sbtdsl.core.Params.Target.RevenjJava

libraryDependencies += "org.revenj" % "revenj-core" % "0.9.4"
