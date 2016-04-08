package com.dslplatform.sbtdsl
package core

import Options.Target

private[core] object TargetInfo {
  val mappings: Map[Target, TargetInfo] = Map(
    Target.RevenjJava -> TargetInfo("revenj.java", "revenj-java.jar"),
    Target.JavaPojo -> TargetInfo("java_pojo", "java-pojo.jar"))
}

private[core] case class TargetInfo(value: String, libname: String)
