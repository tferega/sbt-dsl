package com.dslplatform.sbtdsl.core

private object Templates {
  val TargetGitignore =
    """*.jar
      |""".stripMargin

  def ExampleModule(name: String) =
    s"""module $name {
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

  def SqlScriptDrop(db: String, user: String) =
    s"""-- Terminate all database connections
       |SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$db';
       |
       |-- Drop database
       |DROP DATABASE "$db";
       |
       |-- Drop owner
       |DROP ROLE "$user";
       |""".stripMargin

  def SqlScriptCreate(db: String, user: String, pass: String) =
    s"""-- Create database owner
       |CREATE ROLE "$user" PASSWORD '$pass' NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT LOGIN;
       |
       |-- Create database
       |CREATE DATABASE "$db" OWNER "$user" ENCODING 'utf8' TEMPLATE "template1";
       |""".stripMargin

  def ScalaExample(module: String) =
    s"""import org.example.$module.Book
       |
       |object Example extends App {
       |  println("An example of DSL Platform usage")
       |
       |  println("Creating a new Book...")
       |  val book = new Book("A very interesting book title", 300)
       |
       |  println("Persisting the created book...")
       |  // TODO
       |}
       |""".stripMargin
}
