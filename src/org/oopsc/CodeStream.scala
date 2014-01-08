package org.oopsc

import java.io.File
import java.io.OutputStream
import java.io.PrintStream

/**
 * Data stream printing the assembly code, allows to generate unique markers
 * based upon a custom namespace.
 */
trait CodeStream extends PrintStream {
  /** Current namespace, typically consisting of the class and method name. */
  private var namespace: String = null

  /** Counter for generating unique markers. */
  private var counter = 0

  /**
   * Sets the current namespace.
   *
   * @param namespace Namespace; must be a valid assembly identifier.
   */
  def setNamespace(namespace: String) {
    this.namespace = namespace
    this.counter = 1
  }

  /**
   * Generate a new marker.
   */
  def nextLabel: String = {
    this.counter += 1
    this.namespace + "_" + (this.counter - 1)
  }
}

object CodeStream {
  def apply() = new PrintStream(System.out) with CodeStream
  def apply(stream: OutputStream) = new PrintStream(stream) with CodeStream
  def apply(fileName: String) = new PrintStream(new File(fileName)) with CodeStream
}