package com.dslplatform.sbtdsl.core

import java.nio.file.{ Files, Path, Paths }

trait PathTools {
  def checkFolders(namedPaths: (String, String)*): Seq[String] =
    namedPaths
      .map(parseNamedPath)
      .filter(p => Files.exists(p._2))
      .map(_._2.toString)

  def createPath(path: String): Unit =
    Files.createDirectories(Paths.get(path))

  def writeToFile(str: String, path: String, filename: String): Unit =
    Files.write(Paths.get(path, filename), str.getBytes("UTF-8"))

  def parseNamedPath(namedPath: (String, String)): (String, Path) =
    try {
      (namedPath._1, Paths.get(namedPath._2).toAbsolutePath)
    } catch {
      case e: Exception => throw new IllegalArgumentException(s"""Path ${namedPath._1} is invalid: "${namedPath._2}"""", e)
    }
}
