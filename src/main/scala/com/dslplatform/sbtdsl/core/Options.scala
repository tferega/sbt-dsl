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

  sealed trait Settings
  object Settings {
    case object ActiveRecord extends Settings
    case object Jackson extends Settings
    case object JodaTime extends Settings
    case object ManualJson extends Settings
  }

  sealed trait Target
  object Target {
    case object JavaPojo extends Target
    case object RevenjJava extends Target
  }
}

object Options extends Options
