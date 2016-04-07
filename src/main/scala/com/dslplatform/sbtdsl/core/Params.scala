package com.dslplatform.sbtdsl
package core

object Params {
  sealed trait Target
  object Target {
    case object RevenjJava extends Target
    case object JavaPojo extends Target
  }
}
