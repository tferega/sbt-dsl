package com.dslplatform.sbtdsl
package plugin

import sbt._
import Keys._

object SbtDslPlugin extends AutoPlugin {
  object autoImport {
    val Options = core.Options

    val dslExample = TaskKey[Unit]("dsl-example", "Creates and applies an example DSL model.")
    val dslInit = TaskKey[Unit]("dsl-init", "Initializes the DSL project, doing everything needed to start using it")
    val dslApply = TaskKey[Unit]("dsl-apply", "Apply migration on the database after creating the migration script")

    val dslModule = SettingKey[String]("dsl-module", "Name of the test DSL module created by dslInit, also modifies other dslDb values")
    val dslNamespace = SettingKey[String]("dsl-namespace", "Specifies root namespace for target language (supported by some languages)")
    val dslScm = SettingKey[Options.Scm]("dsl-scm", "Defines which SCM to use when generating directory structure")
    val dslTargets = SettingKey[Seq[Options.Target]]("dsl-targets", "Convert DSL to specified target (Java client, PHP, Revenj server, ...)")

    val dslDbLocation = SettingKey[Options.DbLocation]("dsl-db-location", "Database host and port connection parameters")
    val dslDbName = SettingKey[String]("dsl-db-name", "Database name to connect to")
    val dslDbCredentials = SettingKey[Options.DbCredentials]("dsl-db-credentials", "Username and password to use for database connection")
    val dslSettings = SettingKey[Seq[Options.Settings]]("dsl-settings", "Additional DSL settings")

    val dslTargetPath = SettingKey[String]("dsl-target-path", "Specifies the location of generated artifacts")
    val dslDslPath = SettingKey[String]("dsl-dsl-path", "Specifies location of DSL models folder")
    val dslLibPath = SettingKey[String]("dsl-lib-path", "Specifies location of libraries needed to compile the model")
    val dslSqlPath = SettingKey[String]("dsl-sql-path", "Specifies location of generated SQL scripts")

    val dslCalculatedDb = SettingKey[Options.DbParams]("dsl-calculated-db", "A Utils.DbParams value calculated from: dslModule, dslDbLocation, dslDbName and dslDbCredentials")
    val dslCalculatedPaths = SettingKey[Options.PathParams]("dsl-calculated-paths", "A Utils.Paths value calculated from: dslTargetPath, dslDslPath, dslLibPath, dslSqlPath and sourceDirectory")
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    dslExample := core.Plugin.exampleDsl(streams.value.log, dslNamespace.value, dslScm.value, dslTargets.value, dslCalculatedDb.value, dslSettings.value, dslCalculatedPaths.value),
    dslInit := core.Plugin.initDsl(streams.value.log, dslScm.value, dslCalculatedDb.value, dslCalculatedPaths.value, createExample = false),
    dslApply := core.Plugin.applyDsl(streams.value.log, dslNamespace.value, dslTargets.value, dslCalculatedDb.value, dslSettings.value, dslCalculatedPaths.value),


    dslScm := Options.Scm.Git,
    dslTargets := Nil,

    dslModule := "library",
    dslNamespace := "org.example",
    dslDbLocation := Options.DbLocation("localhost", 5432),
    dslDbName := s"${dslModule.value}_db",
    dslDbCredentials := Options.DbCredentials(s"${dslModule.value}_user", s"${dslModule.value}_pass"),
    dslSettings := Seq(Options.Settings.ManualJson),

    dslTargetPath := "lib",
    dslDslPath := "model/dsl",
    dslLibPath := "model/lib",
    dslSqlPath := "model/sql",

    dslCalculatedDb := Options.DbParams(dslModule.value, dslDbLocation.value, dslDbName.value, dslDbCredentials.value),
    dslCalculatedPaths := Options.PathParams(dslTargetPath.value, dslDslPath.value, dslLibPath.value, dslSqlPath.value, sourceDirectory.value.getPath)
  )
}
