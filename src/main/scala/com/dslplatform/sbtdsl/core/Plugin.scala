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
      checkFolders(
        ("dslTargetPath", paths.target),
        ("dslDslPath", paths.dsl),
        ("dslLibPath", paths.lib),
        ("dslSqlPath", paths.sql))

      try {
        def newPath(path: String): Unit = createPath(path)
        def newFile(str: String, path: String, filename: String): Unit = writeToFile(str, path, filename)
        val scalaExamplePath = Paths.get(paths.sources, "main", "scala").toFile.getPath

        // Create the directory structure.
        newPath(paths.target)
        newPath(paths.dsl)
        newPath(paths.lib)
        newPath(paths.sql)
        newPath(scalaExamplePath)

        // Create SCM ignore files.
        scm match {
          case Scm.Git =>
            newFile(Templates.TargetGitignore, paths.target, ".gitignore")
            newFile(Templates.LibGitignore, paths.lib, ".gitignore")
          case _ =>
        }

        // Create files needed to compile the model.
        newFile(Templates.ExampleModule(db.module), paths.dsl, s"${db.module}.dsl")
        newFile(Templates.SqlScriptDrop(db.name, db.credentials.user), paths.sql, "00-drop-database.sql")
        newFile(Templates.SqlScriptCreate(db.name, db.credentials.user, db.credentials.pass), paths.sql, "10-create-database.sql")
        newFile(Templates.ScalaExample(db.module), scalaExamplePath, "Example.scala")
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
