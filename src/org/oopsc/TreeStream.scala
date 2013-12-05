package org.oopsc

import java.io.OutputStream
import java.io.PrintStream

/**
 * Die Klasse realisiert einen Ausgabestrom, in dem Text baumartig strukturiert
 * werden kann. Da die Klasse von {@link java.io.PrintStream PrintStream} erbt, können alle Methoden
 * verwendet werden, mit denen man auch auf die
 * Konsole schreiben kann. Zusätzlich gibt es Methoden zum Steuern der
 * Einrückungstiefe.
 *
 * @param indentionStep Schrittweite der Einrückung.
 */
class TreeStream(stream: OutputStream, var indentionStep: Int) extends PrintStream(stream) {
  /**
   * Ein Puffer für das zuletzt ausgegebene Zeichen. Falls das letzte Zeichen
   * ein '\n' war, wird vor der Ausgabe des nächsten Zeichens eingerückt.
   */
  private var lastChar = 0

  /** Die aktuelle Einrücktiefe. */
  private var indention = 0

  /**
   * Die Methode erhöht die Einrücktiefe der Ausgabe.
   */
  def indent {
    this.indention += this.indentionStep
  }

  /**
   * Die Methode verringert die Einrücktiefe der Ausgabe.
   */
  def unindent {
    this.indention -= this.indentionStep
    assert(this.indention >= 0)
  }

  /**
   * Die Methode überschreibt die Ausgabemethode der Basisklasse.
   * Sie stellt sicher, dass die Einrückungen vorgenommen werden.
   *
   * @param buf
	 * Der Puffer, der ausgegeben werden soll.
   * @param off
	 * Der Index des ersten Zeichens in dem Puffer, das
   * ausgegeben werden soll.
   * @param len
	 * Die Anzahl der Zeichen, die ausgegeben werden sollen.
   */
  override def write(buf: Array[Byte], off: Int, len: Int) {
    for (i <- off to len - 1) {
      this.write(buf(i))
    }
  }

  /**
   * Die Methode überschreibt die Ausgabemethode der Basisklasse.
   * Sie stellt sicher, dass die Einrückungen vorgenommen werden.
   *
   * @param b
	 * Das auszugebene Zeichen.
   */
  override def write(b: Int) {
    if (this.lastChar == '\n') {
      for (i <- 1 to this.indention) {
        super.write(' ')
      }
    }

    this.lastChar = b
    super.write(b)
  }
}