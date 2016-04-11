package com.dslplatform.sbtdsl

import com.dslplatform.compiler.client.{ Context => ClcContext, Main => ClcMain }
import com.dslplatform.compiler.client.{ parameters => clc }
import java.nio.file.{ Files, Path, Paths }

package object core {
  import Options._

  def compileDsl(
      dbParams: Utils.DbParams,
      targets: Seq[Target],
      paths: Utils.Paths): Unit = {
    val context = new ClcContext()

    // Basic settings
    context.put(clc.Download.INSTANCE, null)
    context.put(clc.Namespace.INSTANCE, "org.example")
    context.put(clc.PostgresConnection.INSTANCE, s"${dbParams.location.host}:${dbParams.location.port}/${dbParams.name}?user=${dbParams.credentials.user}&password=${dbParams.credentials.pass}")

    // Target settings
    targets foreach { target =>
      val info = TargetInfo.mappings(target)
      val path = s"${paths.target}/${info.libname}"
      context.put(info.value, path)
    }

    // Other settings
    context.put(clc.Settings.INSTANCE, "manual-json")

    // Paths and locations
    context.put(clc.DslPath.INSTANCE, paths.dsl)
    context.put(clc.Dependencies.INSTANCE, paths.lib)
    context.put(clc.SqlPath.INSTANCE, paths.sql)

    context.put(clc.ApplyMigration.INSTANCE, null)
    val params = ClcMain.initializeParameters(context, ".")
    ClcMain.processContext(context, params)
  }

  def initDsl(
      scm: Options.Scm,
    paths: Utils.Paths): Unit = {
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

    try {
      // Create the directory structure.
      createPath(paths.target)
      createPath(paths.dsl)
      createPath(paths.lib)
      createPath(paths.sql)

      // Create SCM ignore files.
      scm match {
        case Scm.Git =>
          writeToFile(Templates.TargetGitignore, paths.target, ".gitignore")
          writeToFile(Templates.LibGitignore, paths.lib, ".gitignore")
        case _ =>
      }

      // Create files needed to compile the model.
      writeToFile(Templates.ExampleModule, paths.dsl, "model.dsl")
      writeToFile(Templates.SqlScriptDrop, paths.sql, "00-drop-database.sql")
      writeToFile(Templates.SqlScriptCreate, paths.sql, "10-create-database.sql")

      // Initialize the database
      // TODO
    } catch {
      case e: Exception => throw new RuntimeException(s"An error occurred while creating directory structure: ${e.getMessage}")
    }
  }

  private def checkFolders(namedPaths: (String, String)*): Seq[String] =
    namedPaths
      .map(parseNamedPath)
      .filter(p => Files.exists(p._2))
      .map(_._2.toString)

  private def createPath(path: String): Unit =
    Files.createDirectories(Paths.get(path))

  private def writeToFile(str: String, path: String, filename: String): Unit =
    Files.write(Paths.get(path, filename), str.getBytes("UTF-8"))

  private def parseNamedPath(namedPath: (String, String)): (String, Path) =
    try {
      (namedPath._1, Paths.get(namedPath._2).toAbsolutePath)
    } catch {
      case e: Exception => throw new IllegalArgumentException(s"""Path ${namedPath._1} is invalid: "${namedPath._2}"""")
    }
}
