package org.oopsc.expression

import org.oopsc._
import org.oopsc.symbol._

/**
 * Represents a literal in the syntax tree.
 */
class LiteralExpression(var value: Int, `type`: ClassSymbol, position: Position) extends Expression(position, `type`) {
  def print(tree: TreeStream) {
    tree.println(this.value + " : " + this.`type`.name)
  }

  def generateCode(code: CodeStream) {
    code.println("; " + this.value + " : " + this.`type`.name)

    /* Load value into R5. */
    code.println("MRI R5, " + this.value)

    /* Allocate space on the stack. */
    code.println("ADD R2, R1")

    /* Copy value from R5 to the newly allocated space on the stack. */
    code.println("MMR (R2), R5")
  }

  override def isAlwaysTrue(sem: SemanticAnalysis): Boolean = {
    return this.value == 1 && (this.`type`.isA(sem, Types.intType) || this.`type`.isA(sem, Types.boolType))
  }
}