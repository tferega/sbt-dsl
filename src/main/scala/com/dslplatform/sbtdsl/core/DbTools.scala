package com.dslplatform.sbtdsl.core

import java.sql.{ Connection, DriverManager, ResultSet }

trait DbTools {
  def dbConnect(user: String): Connection = {
    val pass = readln(s"Enter password for role $user")
    val connectionString = s"jdbc:postgresql://localhost:5432/?user=$user&password=$pass"
    try {
      DriverManager.getConnection(connectionString)
    } catch {
      case e: Exception => throw new IllegalStateException(s"Could not establish connection with the database at $connectionString (${e.getMessage})", e)
    }
  }

  def dbDatabaseExists(connection: Connection, name: String): Boolean = {
    val query = s"SELECT count(datname) AS count FROM pg_catalog.pg_database WHERE datname='$name'"
    val r = dbExecuteAndParse(connection, query, rs => rs.getInt("count") == 1)
    r.getOrElse(false)
  }

  def dbRoleExists(connection: Connection, name: String): Boolean = {
    val query = s"SELECT count(rolname) AS count FROM pg_catalog.pg_roles WHERE rolname='$name'"
    val r = dbExecuteAndParse(connection, query, rs => rs.getInt("count") == 1)
    r.getOrElse(false)
  }

  def dbExecuteAndParse[T](
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

  def dbExecute(connection: Connection, query: String): Unit =
    using(connection.createStatement)(_.executeUpdate(query))
}
