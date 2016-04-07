package com.dslplatform.sbtdsl
package core

trait Options {
  sealed trait Target
  object Target {
    case object RevenjJava extends Target
    case object JavaPojo extends Target
  }
}

object Options extends Options
