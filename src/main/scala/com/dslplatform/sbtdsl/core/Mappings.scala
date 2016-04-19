package com.dslplatform.sbtdsl
package core

import Options.{ Settings, Target }

private object Mappings {
  case class TargetInfo(value: String, libname: String)

  val settings: Map[Settings, String] = Map(
    Settings.ActiveRecord -> "active-record",
    Settings.Jackson -> "jackson",
    Settings.JodaTime -> "joda-time",
    Settings.ManualJson -> "manual-json")

  val target: Map[Target, TargetInfo] = Map(
    Target.RevenjJava -> TargetInfo("revenj.java", "revenj-java.jar"),
    Target.JavaPojo -> TargetInfo("java_pojo", "java-pojo.jar"))
}

