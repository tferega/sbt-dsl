package com.dslplatform.sbtdsl
package plugin

import sbt._

object SbtDslPlugin extends AutoPlugin {
  object autoImport extends core.Params
  import autoImport._

  val dslApply = TaskKey[Unit]("dsl-apply", "Apply migration on the database after creating the migration script")
  val dslTargets = SettingKey[Seq[Target]]("dsl-targets", "Convert DSL to specified target (Java client, PHP, Revenj server, ...)")

  override lazy val projectSettings = Seq(
    dslApply := { println("Apply DS: " + dslTargets.value) },
    dslTargets := Nil
  )
}
