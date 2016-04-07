package com.dslplatform.sbtdsl.core

import com.dslplatform.compiler.client

object DslApplicator {
  def apply(targets: Seq[Params.Target]): Unit = {
    val module = "storage"

    val context = new client.Context()
    context.put(client.parameters.Download.INSTANCE, null)
    context.put(client.parameters.DslPath.INSTANCE, "model/dsl")
    context.put(client.parameters.Namespace.INSTANCE, "org.example")
    context.put(client.parameters.Dependencies.INSTANCE, "model/lib")
    context.put("revenj.java", "lib/storage-revenj.jar")
    context.put(client.parameters.Settings.INSTANCE, "manual-json")
    context.put(client.parameters.PostgresConnection.INSTANCE, "localhost:5432/storage_db?user=storage_user&password=storage_pass")
    context.put(client.parameters.SqlPath.INSTANCE, "model/sql")
    context.put(client.parameters.ApplyMigration.INSTANCE, null)
    val params = client.Main.initializeParameters(context, ".")
    client.Main.processContext(context, params)
  }
}
