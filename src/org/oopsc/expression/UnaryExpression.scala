package org.oopsc.expression

import org.oopsc._

object UnaryExpression extends Enumeration {
  type Operator = Value
  val MINUS, NOT = Value
}

/**
 * Represents an expression with an unary operator in the syntax tree.
 */
class UnaryExpression(var operator: UnaryExpression.Operator, var operand: Expression, position: Position) extends Expression(position) {
  import UnaryExpression._

  override def refPass(sem: SemanticAnalysis) {
    this.operand.refPass(sem)

    this.operator match {
      case NOT =>
        this.operand.`type`.check(sem, Types.boolType, this.operand.position)

      case MINUS =>
        this.operand.`type`.check(sem, Types.intType, this.operand.position)
    }

    this.`type` = this.operand.`type`
  }

  def print(tree: TreeStream) {
    tree.println(this.operator.toString + (if (this.`type` == null) "" else " : " + this.`type`.name))
    tree.indent
    this.operand.print(tree)
    tree.unindent
  }

  def generateCode(code: CodeStream) {
    this.operand.generateCode(code, false)

    code.println("; " + this.operator)
    code.println("MRM R5, (R2)")

    this.operator match {
      case NOT =>
        code.println("MRI R6, 1")
        code.println("SUB R6, R5")
        code.println("MMR (R2), R6")

      case MINUS =>
        code.println("MRI R6, 0")
        code.println("SUB R6, R5")
        code.println("MMR (R2), R6")
    }
  }
}