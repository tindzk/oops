package org.oopsc.statement

import org.oopsc.CodeStream
import org.oopsc.SemanticAnalysis
import org.oopsc.TreeStream

abstract class Statement {
  /**
   * Performs the definition pass of the semantic analysis.
   *
   * @param sem Context of the semantic analysis.
   */
  def refPass(sem: SemanticAnalysis) {

  }

  /**
   * Performs the optimisation pass.
   *
   * @return Optimised statement.
   */
  def optimPass() = this

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