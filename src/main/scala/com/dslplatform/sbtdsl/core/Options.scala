package com.dslplatform.sbtdsl
package core

trait Options {
  case class DbLocation(host: String, port: Int)
  case class DbCredentials(user: String, pass: String)

  sealed trait Scm
  object Scm {
    case object None extends Scm
    case object Git extends Scm
  }

  sealed trait Target
  object Target {
    case object RevenjJava extends Target
    case object JavaPojo extends Target
  }
}

object Options extends Options
