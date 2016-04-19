package com.dslplatform.sbtdsl
package core

private object Utils {
  def readln(prompt: String): String = {
    print(s"$prompt: ")
    System.out.flush()
    val line = readLine()
    println
    line
  }

  def using[C <: AutoCloseable, R](closeable: C)(f: C => R): R =
    try {
      f(closeable)
    } finally {
      closeable.close()
    }
}
