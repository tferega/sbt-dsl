package com.dslplatform.sbtdsl
package core

trait Params {
  sealed trait Target
  object Target {
    case object RevenjJava extends Target
    case object JavaPojo extends Target
  }
}

object Params extends Params
