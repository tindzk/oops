package org.oopsc.symbol

import org.oopsc._

abstract class Symbol(var identifier: Identifier) {
  var accessLevel = AccessLevel.Public
  var declaringClass: Option[ClassSymbol] = None

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

  def availableFor(clazz: Option[ClassSymbol]) = {
    ((this.accessLevel == AccessLevel.Private) &&
      (clazz.isDefined && (this.declaringClass.get eq clazz.get))) ||
      (this.accessLevel == AccessLevel.Protected &&
        (clazz.isDefined && this.declaringClass.get.isA(clazz.get))) ||
      (this.accessLevel == AccessLevel.Public)
  }
}