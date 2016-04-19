package com.dslplatform.sbtdsl

package object core {
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
