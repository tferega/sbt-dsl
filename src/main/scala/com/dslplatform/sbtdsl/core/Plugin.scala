package com.dslplatform.sbtdsl.core

import Options._

import com.dslplatform.compiler.client.{ Context => ClcContext, Main => ClcMain }
import com.dslplatform.compiler.client.{ parameters => clc }
import java.nio.file.Paths

object Plugin extends DbTools with PathTools {
  def initAndApplyDsl(
      namespace: String,
      scm: Options.Scm,
      targets: Seq[Target],
      db: DbParams,
      settings: Seq[Options.Settings],
      paths: PathParams): Unit = {
    initDsl(scm, db, paths)
    applyDsl(namespace, targets, db, settings, paths)
  }

  def initDsl(
      scm: Options.Scm,
      db: DbParams,
      paths: PathParams): Unit = {
    // Make sure that postgres driver is registered.
    Class.forName("org.postgresql.Driver")

    // Initialize database connection.
    using(dbConnect("postgres")) { connection =>
      // Check that dsl paths are not invalid, and that they do not already exist.
      val existingFolders = checkFolders(
        ("dslTargetPath", paths.target),
        ("dslDslPath", paths.dsl),
        ("dslLibPath", paths.lib),
        ("dslSqlPath", paths.sql))
      if (existingFolders.nonEmpty) {
        val folderReport = existingFolders mkString ", "
        throw new IllegalStateException(s"Project directory structure already initialized: $folderReport")
      }

      // Check that the database model has not be already created.
      if (dbDatabaseExists(connection, db.name) || dbRoleExists(connection, db.credentials.user)) {
        throw new IllegalStateException(s"Database user or model already initialized (${db.credentials.user} / ${db.name})")
      }

      try {
        val scalaExamplePath = Paths.get(paths.sources, "main", "scala").toFile.getPath

        // Create the directory structure.
        createPath(paths.target)
        createPath(paths.dsl)
        createPath(paths.lib)
        createPath(paths.sql)
        createPath(scalaExamplePath)

        // Create SCM ignore files.
        scm match {
          case Scm.Git =>
            writeToFile(Templates.TargetGitignore, paths.target, ".gitignore")
            writeToFile(Templates.LibGitignore, paths.lib, ".gitignore")
          case _ =>
        }

        // Create files needed to compile the model.
        writeToFile(Templates.ExampleModule(db.module), paths.dsl, s"${db.module}.dsl")
        writeToFile(Templates.SqlScriptDrop(db.name, db.credentials.user), paths.sql, "00-drop-database.sql")
        writeToFile(Templates.SqlScriptCreate(db.name, db.credentials.user, db.credentials.pass), paths.sql, "10-create-database.sql")
        writeToFile(Templates.ScalaExample(db.module), scalaExamplePath, "Example.scala")
      } catch {
        case e: Exception => throw new RuntimeException(s"An error occurred while creating directory structure: ${e.getMessage}", e)
      }

      try {
        // Create the database user and model.
        dbExecute(connection, Templates.SqlScriptCreate(db.name, db.credentials.user, db.credentials.pass))
      } catch {
        case e: Exception => throw new RuntimeException(s"An error occurred while creating the database model: ${e.getMessage}", e)
      }
    }
  }

  def applyDsl(
      namespace: String,
      targets: Seq[Target],
      db: DbParams,
      settings: Seq[Options.Settings],
      paths: PathParams): Unit = {
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
