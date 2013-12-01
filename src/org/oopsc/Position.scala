package org.oopsc

class Position(var line: Int, var column: Int) {
  override def toString() = s"line $line, column $column"
}