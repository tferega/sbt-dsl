package com.dslplatform.sbtdsl.core

object Templates {
  val ModuleName = "test"
  val DbName = "test_db"
  val UserName = "test_user"
  val UserPass = "test_pass"

  val TargetGitignore =
    """*.jar
      |""".stripMargin

  val ExampleModule =
    s"""module $ModuleName {
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
    s"""-- Terminate all database connections
       |SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DbName';
       |
       |-- Drop database
       |DROP DATABASE "$DbName";
       |
       |-- Drop owner
       |DROP ROLE "$UserName";
       |""".stripMargin

  val SqlScriptCreate =
    s"""-- Create database owner
       |CREATE ROLE "$UserName" PASSWORD '$UserPass' NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT LOGIN;
       |
       |-- Create database
       |CREATE DATABASE "$DbName" OWNER "$UserName" ENCODING 'utf8' TEMPLATE "template1";
       |""".stripMargin
}
