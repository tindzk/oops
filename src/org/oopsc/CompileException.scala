package org.oopsc

/**
 * Die Klasse repräsentiert die Ausnahme, die bei Übersetzungsfehlern erzeugt wird.
 * Sie wird in der Hauptmethode {@link OOPSC#main(String[]) OOPSC.main} gefangen und
 * ausgegeben.
 */
class CompileException(message: String) extends Exception("Fehler: " + message) {
  /**
   * Konstruktor.
   *
   * @param message
   * Die Fehlermeldung. Ihr wird der Text "Fehler in Zeile x,
   *       Spalte y: " vorangestellt, bzw. lediglich "Fehler: ", wenn die
   *       Quelltextstelle unbekannt ist.
   * @param position
   * Die Quelltextstelle an der der Fehler aufgetreten ist.
   *       Dieser Parameter kann auch null sein, wenn die Stelle nicht
   *       zugeordnet werden kann.
   */
  def this(message: String, position: Position) = this("Error in line " + position.line
    + ", column " + position.column + ": " + message)
}