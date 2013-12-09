package org.oopsc.expression

import org.oopsc._
import org.oopsc.symbol.ClassSymbol

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
        this.operand.resolvedType().check(sem, Types.boolType, this.operand.position)

      case MINUS =>
        this.operand.resolvedType().check(sem, Types.intType, this.operand.position)
    }
  }

  override def optimPass() : Expression = {
    this.operand.optimPass() match {
      case o: BooleanLiteralExpression =>
        this.operator match {
          case NOT =>
            val value = !o.value
            BooleanLiteralExpression(value, this.position)
        }

      case o: IntegerLiteralExpression =>
        this.operator match {
          case MINUS =>
            val value = -o.value
            IntegerLiteralExpression(value, this.position)
        }

      case o: UnaryExpression =>
        if (o.operator == this.operator == MINUS) {
          /* -(-x) → x */
          o.operand
        } else if (o.operator == this.operator == NOT) {
          /* NOT (NOT x) → x */
          o.operand
        } else {
          o
        }

      case o => o
    }
  }

  override def resolvedType() : ClassSymbol = this.operand.resolvedType()

  def print(tree: TreeStream) {
    tree.println(this.operator.toString + " : " + this.resolvedType().name)
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