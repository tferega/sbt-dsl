package com.dslplatform.sbtdsl
package plugin

import core.Utils
import sbt._

object SbtDslPlugin extends AutoPlugin {
  object autoImport {
    val Options = core.Options

    val dslApply = TaskKey[Unit]("dsl-apply", "Apply migration on the database after creating the migration script")

    val dslModuleName = SettingKey[String]("dsl-module-name", "String used to fill default values for some other settings")
    val dslTargets = SettingKey[Seq[Options.Target]]("dsl-targets", "Convert DSL to specified target (Java client, PHP, Revenj server, ...)")

    val dslDbLocation = SettingKey[Options.DbLocation]("dsl-db-location", "Database host and port connection parameters")
    val dslDbName = SettingKey[String]("dsl-db-name", "Database name to connect to")
    val dslDbCredentials = SettingKey[Options.DbCredentials]("dsl-db-credentials", "Username and password to use for database connection")

    val dslTargetPath = SettingKey[String]("dsl-target-path", "Specifies the location of generated artifacts")
    val dslDslPath = SettingKey[String]("dsl-dsl-path", "Specifies location of DSL models folder")
    val dslLibPath = SettingKey[String]("dsl-lib-path", "Specifies location of libraries needed to compile the model")
    val dslSqlPath = SettingKey[String]("dsl-sql-path", "Specifies location of generated SQL scripts")
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    dslApply := core.compileDsl(
      dbParams = Utils.DbParams(dslDbLocation.value, dslDbName.value, dslDbCredentials.value),
      targets = dslTargets.value,
      paths = Utils.Paths(dslTargetPath.value, dslDslPath.value, dslLibPath.value, dslSqlPath.value)),

    dslModuleName := "TestModule",
    dslTargets := Nil,

    dslDbLocation := Options.DbLocation("localhost", 5432),
    dslDbName := s"${dslModuleName.value}_db",
    dslDbCredentials := Options.DbCredentials(s"${dslModuleName.value}_user", s"${dslModuleName.value}_pass"),

    dslTargetPath := "lib",
    dslDslPath := "model/dsl",
    dslLibPath := "model/lib",
    dslSqlPath := "model/sql"
  )
}
