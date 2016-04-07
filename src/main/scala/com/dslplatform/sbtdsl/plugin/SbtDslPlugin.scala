package com.dslplatform.sbtdsl
package plugin

import sbt._

object SbtDslPlugin extends AutoPlugin {
  object autoImport {
    val Options = core.Options

    val dslApply = TaskKey[Unit]("dsl-apply", "Apply migration on the database after creating the migration script")

    val dslTargets = SettingKey[Seq[Options.Target]]("dsl-targets", "Convert DSL to specified target (Java client, PHP, Revenj server, ...)")
    val dslTargetPath = SettingKey[String]("dsl-target-path", "Specifies the location of generated artifacts")
    val dslDslPath = SettingKey[String]("dsl-dsl-path", "Specifies location of DSL models folder")
    val dslLibPath = SettingKey[String]("dsl-lib-path", "Specifies location of libraries needed to compile the model")
    val dslSqlPath = SettingKey[String]("dsl-sql-path", "Specifies location of generated SQL scripts")
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    dslApply := core.compileDsl(
      targets = dslTargets.value,
      paths = Options.Paths(dslTargetPath.value, dslDslPath.value, dslLibPath.value, dslSqlPath.value)),

    dslTargets := Nil,
    dslTargetPath := "lib",
    dslDslPath := "model/dsl",
    dslLibPath := "model/lib",
    dslSqlPath := "model/sql"
  )
}
