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
   * Prints declaration in a tree structure.
   *
   * @param tree Output stream.
   */
  def print(tree: TreeStream)

  def availableFor(clazz: Option[ClassSymbol]) = {
    this.accessLevel match {
      case AccessLevel.Public => true
      case AccessLevel.Private =>
        (clazz, this.declaringClass) match {
          case (Some(c), Some(c2)) => c eq c2
          case _ => false
        }
      case AccessLevel.Protected =>
        (clazz, this.declaringClass) match {
          /* isA() checks the class hierarchy (as opposed to a simple reference check with `eq'). */
          case (Some(c), Some(c2)) => c.isA(c2)
          case _ => false
        }
    }
  }
}