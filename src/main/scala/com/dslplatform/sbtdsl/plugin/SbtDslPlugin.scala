package com.dslplatform.sbtdsl
package plugin

import sbt._

object SbtDslPlugin extends AutoPlugin {
  object autoImport {
    val Options = core.Options
    val dslApply = TaskKey[Unit]("dsl-apply", "Apply migration on the database after creating the migration script")
    val dslTargets = SettingKey[Seq[Options.Target]]("dsl-targets", "Convert DSL to specified target (Java client, PHP, Revenj server, ...)")
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    dslApply := core.compileDsl(),
    dslTargets := Nil
  )
}
