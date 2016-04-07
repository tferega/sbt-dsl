package com.dslplatform.sbtdsl

import com.dslplatform.compiler.client

package object core {
  def compileDsl(paths: Options.Paths): Unit = {
    val context = new client.Context()

    // Basic settings
    context.put(client.parameters.Download.INSTANCE, null)
    context.put(client.parameters.Namespace.INSTANCE, "org.example")
    context.put(client.parameters.PostgresConnection.INSTANCE, "localhost:5432/storage_db?user=storage_user&password=storage_pass")

    // Target settings
    context.put("revenj.java", "lib/storage-revenj.jar")

    // Other settings
    context.put(client.parameters.Settings.INSTANCE, "manual-json")

    // Paths and locations
    context.put(client.parameters.DslPath.INSTANCE, paths.dsl)
    context.put(client.parameters.Dependencies.INSTANCE, paths.lib)
    context.put(client.parameters.SqlPath.INSTANCE, paths.sql)

    context.put(client.parameters.ApplyMigration.INSTANCE, null)
    val params = client.Main.initializeParameters(context, ".")
    client.Main.processContext(context, params)
  }
}
