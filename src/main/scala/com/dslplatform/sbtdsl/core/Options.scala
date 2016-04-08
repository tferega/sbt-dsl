package com.dslplatform.sbtdsl
package core

trait Options {
  case class DbLocation(host: String, port: Int)
  case class DbCredentials(user: String, pass: String)

  sealed trait Target
  object Target {
    case object RevenjJava extends Target
    case object JavaPojo extends Target
  }
}

object Options extends Options
