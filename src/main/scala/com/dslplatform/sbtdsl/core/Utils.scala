package com.dslplatform.sbtdsl
package core

object Utils {
  case class DbParams(module: String, location: Options.DbLocation, name: String, credentials: Options.DbCredentials)
  case class Paths(target: String, dsl: String, lib: String, sql: String)
}
