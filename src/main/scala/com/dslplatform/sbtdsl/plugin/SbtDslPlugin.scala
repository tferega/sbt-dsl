package com.dslplatform.sbtdsl
package plugin

import sbt._

object SbtDslPlugin extends AutoPlugin {
  object autoImport {
    sealed trait Target
    object Target {
      case object RevenjJava extends Target
      case object JavaPojo extends Target
    }

    lazy val dslTargets = SettingKey[Seq[Target]]("dsl-targets", "Convert DSL to specified target (Java client, PHP, Revenj server, ...)")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    dslTargets := Nil
  )
}
