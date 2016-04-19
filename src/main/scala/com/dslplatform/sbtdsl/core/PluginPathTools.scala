package com.dslplatform.sbtdsl.core

import java.nio.file.{ Files, Paths }

trait PluginPathTools {
  def checkPaths(namedPaths: (String, String)*): Unit =
    namedPaths foreach { namedPath =>
      try {
        (namedPath._1, Paths.get(namedPath._2).toAbsolutePath)
      } catch {
        case e: Exception => throw new IllegalArgumentException(s"""Path ${namedPath._1} is invalid: "${namedPath._2}"""", e)
      }
    }

  def createPath(path: String): Boolean = {
    val p = Paths.get(path)
    if (Files.exists(p)) {
      false
    } else {
      Files.createDirectories(p)
      true
    }
  }

  def writeToPath(str: String, path: String, filename: String): Boolean = {
    val p = Paths.get(path, filename)
    if (Files.exists(p)) {
      false
    } else {
      Files.write(p, str.getBytes("UTF-8"))
      true
    }
  }
}
