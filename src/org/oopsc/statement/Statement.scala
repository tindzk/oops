package org.oopsc.statement

import org.oopsc.CodeStream
import org.oopsc.SemanticAnalysis
import org.oopsc.TreeStream

abstract class Statement {
  /**
   * Die Methode führt die Kontextanalyse für diese Anweisung durch.
   *
   * @param sem
   * Die an dieser Stelle gültigen Deklarationen.
   * @throws CompileException
   * Während der Kontextanalyse wurde ein Fehler
   * gefunden.
   */
  def refPass(sem: SemanticAnalysis) {

  }

  /**
   * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
   *
   * @param tree
   * Der Strom, in den die Ausgabe erfolgt.
   */
  def print(tree: TreeStream)

  /**
   * Die Methode generiert den Assembler-Code für diese Anweisung. Sie geht
   * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
   *
   * @param code
   * Der Strom, in den die Ausgabe erfolgt.
   * @param tryContexts
   * Current number of TRY blocks. May be used to inject instructions for unwinding
   * the stack (as needed for RETURN statements in TRY blocks).
   */
  def generateCode(code: CodeStream, tryContexts: Int)
}