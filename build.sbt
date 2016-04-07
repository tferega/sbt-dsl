organization := "com.dslplatform"
name := "sbt-dsl"
version := "0.0.1-SNAPSHOT"

sbtPlugin := true

unmanagedSourceDirectories in Compile := Seq((scalaSource in Compile).value)

libraryDependencies += "com.dslplatform" % "dsl-clc" % "1.6.1"
