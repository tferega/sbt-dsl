package com.dslplatform.sbtdsl
package core

import Utils._

import java.sql.{ Connection, DriverManager, ResultSet }

private object PluginDbTools {
  def dbConnect(user: String): Connection = {
    val pass = readln(s"Enter password for role $user")
    val connectionString = s"jdbc:postgresql://localhost:5432/?user=$user&password=$pass"
    try {
      DriverManager.getConnection(connectionString)
    } catch {
      case e: Exception => throw new IllegalStateException(s"Could not establish connection with the database at $connectionString (${e.getMessage})", e)
    }
  }

  def dbRoleExists(connection: Connection, roleName: String): Boolean = {
    val query = s"""SELECT count(rolname) AS count FROM pg_catalog.pg_roles WHERE rolname='$roleName';"""
    val r = dbExecuteAndParse(connection, query, rs => rs.getInt("count") == 1)
    r.getOrElse(false)
  }

  def dbCreateRole(connection: Connection, roleName: String, rolePass: String): Unit = {
    val query = s"""CREATE ROLE "$roleName" PASSWORD '$rolePass' NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT LOGIN;"""
    dbExecute(connection, query)
  }

  def dbModelExists(connection: Connection, modelName: String): Boolean = {
    val query = s"""SELECT count(datname) AS count FROM pg_catalog.pg_database WHERE datname='$modelName';"""
    val r = dbExecuteAndParse(connection, query, rs => rs.getInt("count") == 1)
    r.getOrElse(false)
  }

  def dbCreateModel(connection: Connection, modelName: String, roleName: String): Unit = {
    val query = s"""CREATE DATABASE "$modelName" OWNER "$roleName" ENCODING 'utf8' TEMPLATE "template1";"""
    dbExecute(connection, query)
  }

  private def dbExecuteAndParse[T](
      connection: Connection,
      query: String,
      rsParser: ResultSet => T): Option[T] =
    using(connection.createStatement) { statement =>
      using(statement.executeQuery(query)) { rs =>
        if (rs.next) {
          Some(rsParser(rs))
        } else {
          None
        }
      }
    }

  private def dbExecute(connection: Connection, query: String): Unit =
    using(connection.createStatement)(_.executeUpdate(query))
}
