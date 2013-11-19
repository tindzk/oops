package org.oopsc.symbol

import org.oopsc._

abstract class Symbol(var identifier: Identifier) {
  def name(): String = identifier.name

  /**
   * Performs the definition pass of the semantic analysis.
   *
   * @param sem Context of the semantic analysis.
   */
  def defPass(sem: SemanticAnalysis)

  /**
   * Performs the reference pass of the semantic analysis.
   *
   * @param sem Context of the semantic analysis.
   */
  def refPass(sem: SemanticAnalysis)

  /**
   * Die Methode gibt diese Deklaration in einer Baumstruktur aus.
   *
   * @param tree
   * Der Strom, in den die Ausgabe erfolgt.
   */
  def print(tree: TreeStream)
}