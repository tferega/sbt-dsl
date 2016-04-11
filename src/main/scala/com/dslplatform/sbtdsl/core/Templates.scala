package com.dslplatform.sbtdsl.core

object Templates {
  val TargetGitignore =
    """*.jar
      |""".stripMargin

  val ExampleModule =
    """module test {
      |  aggregate Book {
      |    String name;
      |    Int pageCount;
      |  }
      |}
      |""".stripMargin

  val LibGitignore =
    """revenj.java
      |java_pojo
      |""".stripMargin

  val SqlScriptDrop =
    """-- Terminate all database connections
      |SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'test_db';
      |
      |-- Drop database
      |DROP DATABASE "test_db";
      |
      |-- Drop owner
      |DROP ROLE "test_user";
      |""".stripMargin

  val SqlScriptCreate =
    """-- Create database owner
      |CREATE ROLE "test_user" PASSWORD 'test_pass' NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT LOGIN;
      |
      |-- Create database
      |CREATE DATABASE "test_db" OWNER "test_user" ENCODING 'utf8' TEMPLATE "template1";
      |""".stripMargin
}
