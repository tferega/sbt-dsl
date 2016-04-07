package com.dslplatform.sbtdsl.core

object DslApplicator {
  def apply(targets: Seq[Params.Target]): Unit = {
    println("Applying DSL: " + targets);
  }
}
