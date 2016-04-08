package com.dslplatform.sbtdsl

import com.dslplatform.compiler.client.{ Context => ClcContext, Main => ClcMain }
import com.dslplatform.compiler.client.{ parameters => clc }

package object core {
  import Options._

  def compileDsl(targets: Seq[Target], paths: Utils.Paths): Unit = {
    val context = new ClcContext()

    // Basic settings
    context.put(clc.Download.INSTANCE, null)
    context.put(clc.Namespace.INSTANCE, "org.example")
    context.put(clc.PostgresConnection.INSTANCE, "localhost:5432/storage_db?user=storage_user&password=storage_pass")

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
}
