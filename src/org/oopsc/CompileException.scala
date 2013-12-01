package org.oopsc

object CompileException {
  def formatError(position: Position, errorLine: String, errorStart: Int, errorEnd: Int) {
    var out = errorLine + "\n"

    for (i <- 0 to position.column) {
      out += " "
    }

    out += "\n"

    if (errorStart >= 0 && errorEnd >= 0) {
      for (i <- errorStart to errorEnd) {
        out += "^"
      }
    }
  }
}

/**
 * Die Klasse repräsentiert die Ausnahme, die bei Übersetzungsfehlern erzeugt wird.
 * Sie wird in der Hauptmethode {@link OOPSC#main(String[]) OOPSC.main} gefangen und
 * ausgegeben.
 */
class CompileException(message: String) extends Exception("Error: " + message) {
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
  def this(message: String, position: Position) = this("Error in " + position + ": " + message)

  def this(message: String, position: Position, errorLine: String, errorStart: Int, errorEnd: Int) = {
    this("Error in " + position + ": " + message + "\n" + CompileException.formatError(position, errorLine, errorStart, errorEnd))
  }
}