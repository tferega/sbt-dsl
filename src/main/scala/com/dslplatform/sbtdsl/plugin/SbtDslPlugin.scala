package com.dslplatform.sbtdsl
package plugin

import core.Utils
import sbt._

object SbtDslPlugin extends AutoPlugin {
  object autoImport {
    val Options = core.Options

    val dslInit = TaskKey[Unit]("dsl-init", "Initializes the DSL project, doing everything needed to start using it")
    val dslCompile = TaskKey[Unit]("dsl-apply", "Apply migration on the database after creating the migration script")

    val dslScm = SettingKey[Options.Scm]("dsl-scm", "Defines which SCM to use when generating directory structure")
    val dslTargets = SettingKey[Seq[Options.Target]]("dsl-targets", "Convert DSL to specified target (Java client, PHP, Revenj server, ...)")

    val dslDbLocation = SettingKey[Options.DbLocation]("dsl-db-location", "Database host and port connection parameters")
    val dslDbName = SettingKey[String]("dsl-db-name", "Database name to connect to")
    val dslDbCredentials = SettingKey[Options.DbCredentials]("dsl-db-credentials", "Username and password to use for database connection")

    val dslTargetPath = SettingKey[String]("dsl-target-path", "Specifies the location of generated artifacts")
    val dslDslPath = SettingKey[String]("dsl-dsl-path", "Specifies location of DSL models folder")
    val dslLibPath = SettingKey[String]("dsl-lib-path", "Specifies location of libraries needed to compile the model")
    val dslSqlPath = SettingKey[String]("dsl-sql-path", "Specifies location of generated SQL scripts")

    lazy val dslCalculatedDb = SettingKey[Utils.DbParams]("dsl-calculated-db", "")
    lazy val dslCalculatedPaths = SettingKey[Utils.Paths]("dsl-calculated-paths", "")
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    dslInit := core.initAndCompileDsl(dslScm.value, dslTargets.value, dslCalculatedDb.value, dslCalculatedPaths.value),
    dslCompile := core.compileDsl(dslTargets.value, dslCalculatedDb.value, dslCalculatedPaths.value),

    dslScm := Options.Scm.Git,
    dslTargets := Nil,

    dslDbLocation := Options.DbLocation("localhost", 5432),
    dslDbName := "test_db",
    dslDbCredentials := Options.DbCredentials("test_user", "test_pass"),

    dslTargetPath := "lib",
    dslDslPath := "model/dsl",
    dslLibPath := "model/lib",
    dslSqlPath := "model/sql",

    dslCalculatedDb := Utils.DbParams(dslDbLocation.value, dslDbName.value, dslDbCredentials.value),
    dslCalculatedPaths := Utils.Paths(dslTargetPath.value, dslDslPath.value, dslLibPath.value, dslSqlPath.value)
  )
}
