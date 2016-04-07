package com.dslplatform.sbtdsl
package plugin

import core._
import sbt._

object SbtDslPlugin extends AutoPlugin {
  object autoImport {
    val dslApply = TaskKey[Unit]("dsl-apply", "Apply migration on the database after creating the migration script")
    val dslTargets = SettingKey[Seq[Params.Target]]("dsl-targets", "Convert DSL to specified target (Java client, PHP, Revenj server, ...)")
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    dslApply := core.DslApplicator(dslTargets.value),
    dslTargets := Nil
  )
}
