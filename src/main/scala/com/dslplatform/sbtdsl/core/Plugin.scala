package com.dslplatform.sbtdsl
package core

import Options._
import PluginDbTools._
import PluginPathTools._
import Utils._

import com.dslplatform.compiler.client.{ Context => ClcContext, Main => ClcMain }
import com.dslplatform.compiler.client.{ parameters => clc }
import java.nio.file.Paths
import sbt.Logger

object Plugin {
  def initAndApplyDsl(
      logger: Logger,
      namespace: String,
      scm: Options.Scm,
      targets: Seq[Target],
      db: DbParams,
      settings: Seq[Options.Settings],
      paths: PathParams): Unit = {
    initDsl(logger, scm, db, paths)
    applyDsl(logger, namespace, targets, db, settings, paths)
  }

  def initDsl(
      logger: Logger,
      scm: Options.Scm,
      db: DbParams,
      paths: PathParams): Unit = {
    logger.info("Initializing the DSL project")
    logger.debug(s"SCM: $scm")
    logger.debug(s"DB params: $db")
    logger.debug(s"Paths: $paths")

    // Make sure that postgres driver is registered.
    Class.forName("org.postgresql.Driver")

    logger.debug("Initializing database connection...")
    using(dbConnect("postgres")) { connection =>
      logger.debug("Verifying DSL paths...")
      checkPaths(
        ("dslTargetPath", paths.target),
        ("dslDslPath", paths.dsl),
        ("dslLibPath", paths.lib),
        ("dslSqlPath", paths.sql))

      try {
        def newFolder(path: String): Unit = if (createPath(path)) { logger.warn(s"Folder $path already exists; not creating!") } else { logger.debug(s"Created path $path") }
        def newFile(str: String, path: String, filename: String): Unit = if (writeToPath(str, path, filename)) { logger.warn(s"File $path already exists; not overwriting!") } else { logger.debug(s"Created file $path") }
        val scalaExamplePath = Paths.get(paths.sources, "main", "scala").toFile.getPath

        logger.debug("Creating the project directory structure...")
        newFolder(paths.target)
        newFolder(paths.dsl)
        newFolder(paths.lib)
        newFolder(paths.sql)
        newFolder(scalaExamplePath)

        logger.debug("Creating SCM ignore files....")
        scm match {
          case Scm.Git =>
            newFile(Templates.TargetGitignore, paths.target, ".gitignore")
            newFile(Templates.LibGitignore, paths.lib, ".gitignore")
          case _ =>
        }

        logger.debug("Creating files needed to compile the model...")
        newFile(Templates.ExampleModule(db.module), paths.dsl, s"${db.module}.dsl")
        newFile(Templates.SqlScriptDrop(db.name, db.credentials.user), paths.sql, "00-drop-database.sql")
        newFile(Templates.SqlScriptCreate(db.name, db.credentials.user, db.credentials.pass), paths.sql, "10-create-database.sql")
        newFile(Templates.ScalaExample(db.module), scalaExamplePath, "Example.scala")
      } catch {
        case e: Exception => throw new RuntimeException(s"An error occurred while creating project directory structure and needed files: ${e.getMessage}", e)
      }

      try {
        logger.debug("Creating the database user and model....")

        val role = db.credentials.user
        if (dbRoleExists(connection, role)) {
          logger.warn(s"Role $role already exists; not creating!")
        } else {
          dbCreateRole(connection, role, db.credentials.pass)
          logger.debug(s"Created role $role")
        }

        val model = db.name
        if (dbModelExists(connection, model)) {
          logger.warn(s"Model $model already exists; not creating!")
        } else {
          dbCreateModel(connection, model, role)
          logger.debug(s"Created model $model")
        }
      } catch {
        case e: Exception => throw new RuntimeException(s"An error occurred while creating the database user or model: ${e.getMessage}", e)
      }
    }
  }

  def applyDsl(
      logger: Logger,
      namespace: String,
      targets: Seq[Target],
      db: DbParams,
      settings: Seq[Options.Settings],
      paths: PathParams): Unit = {
    logger.info("Applying DSL")
    val context = new ClcContext()

    // Basic settings
    context.put(clc.ApplyMigration.INSTANCE, null)
    context.put(clc.Download.INSTANCE, null)
    context.put(clc.Namespace.INSTANCE, namespace)
    context.put(clc.PostgresConnection.INSTANCE, s"${db.location.host}:${db.location.port}/${db.name}?user=${db.credentials.user}&password=${db.credentials.pass}")

    // Target settings
    targets foreach { target =>
      val info = Mappings.target(target)
      val path = s"${paths.target}/${db.module}-${info.libname}"
      context.put(info.value, path)
    }

    // Other settings
    settings foreach { setting =>
      val value = Mappings.settings(setting)
      context.put(clc.Settings.INSTANCE, value)
    }

    // Paths and locations
    context.put(clc.DslPath.INSTANCE, paths.dsl)
    context.put(clc.Dependencies.INSTANCE, paths.lib)
    context.put(clc.SqlPath.INSTANCE, paths.sql)

    val params = ClcMain.initializeParameters(context, ".")
    ClcMain.processContext(context, params)
  }
}
