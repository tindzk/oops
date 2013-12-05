package org.oopsc

import java.io.File
import java.io.OutputStream
import java.io.PrintStream

/**
 * Die Klasse repräsentiert einen Datenstrom, in der Assemblercode des
 * auszugebenen Programms geschrieben wird. Da die Klasse von {@link java.io.PrintStream
 * PrintStream} erbt, können alle Methoden
 * verwendet werden, mit denen man auch auf die Konsole schreiben kann.
 * Zusätzlich kann die Klasse eindeutige Marken für den Assemblerquelltext
 * generieren.
 */
trait CodeStream extends PrintStream {
  /** Das Attribut enthält den gerade gültigen Namensraum (Klasse + Methode). */
  private var namespace: String = null

  /** Das Attribut ist ein Zähler zur Generierung eindeutiger Bezeichner. */
  private var counter: Int = 0

  /**
   * Die Methode setzt den aktuell gültigen Namensraum.
   * Dieser wird verwendet, um eindeutige Marken zu generieren.
   * Derselbe Namensraum darf nur einmal während der Code-Erzeugung
   * gesetzt werden.
   *
   * @param namespace
	 * Den ab jetzt gültigen Namensraum (Klasse + Methode).
   */
  def setNamespace(namespace: String) {
    this.namespace = namespace
    this.counter = 1
  }

  /**
   * Die Methode erzeugt eine eindeutige Marke im aktuellen Namensraum.
   *
   * @return Die Marke.
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