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
   * Prints statement in a tree structure.
   *
   * @param tree Output stream.
   */
  def print(tree: TreeStream)

  /**
   * Generates assembly code for the statement. Requires prior completion of the
   * contextual analysis.
   *
   * @param code Output stream.
   * @param tryContexts Current number of TRY blocks. May be used to inject instructions
   *                    for unwinding the stack (as needed for RETURN statements in TRY blocks).
   */
  def generateCode(code: CodeStream, tryContexts: Int)
}