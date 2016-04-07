package com.dslplatform.sbtdsl
package core

trait Options {
  case class Paths(target: String, dsl: String, lib: String, sql: String)

  sealed trait Target
  object Target {
    case object RevenjJava extends Target
    case object JavaPojo extends Target
  }
}

object Options extends Options
