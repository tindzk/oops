package org.oopsc.expression

import org.oopsc._
import org.oopsc.symbol.ClassSymbol

object BinaryExpression extends Enumeration {
  type Operator = Value
  val EQ, NEQ, GT, GTEQ, LT, LTEQ, PLUS, MINUS, MUL, DIV, MOD, AND, OR, NOT = Value
}

/**
 * Represents an expression with a binary operator in the syntax tree.
 */
class BinaryExpression(var leftOperand: Expression, var operator: BinaryExpression.Operator, var rightOperand: Expression) extends Expression(leftOperand.position) {
  import BinaryExpression._

  override def refPass(sem: SemanticAnalysis) {
    this.leftOperand.refPass(sem)
    this.rightOperand.refPass(sem)

    this.operator match {
      case AND | OR =>
        this.leftOperand.`type`.check(sem, Types.boolType, this.leftOperand.position)
        this.rightOperand.`type`.check(sem, Types.boolType, this.rightOperand.position)
        this.`type` = Types.boolType

      case PLUS | MINUS | MUL | DIV | MOD =>
        this.leftOperand.`type`.check(sem, Types.intType, this.leftOperand.position)
        this.rightOperand.`type`.check(sem, Types.intType, this.rightOperand.position)
        this.`type` = Types.intType

      case GT | GTEQ | LT | LTEQ =>
        this.leftOperand.`type`.check(sem, Types.intType, this.leftOperand.position)
        this.rightOperand.`type`.check(sem, Types.intType, this.rightOperand.position)
        this.`type` = Types.boolType

      case EQ | NEQ =>
        if (!this.leftOperand.`type`.isA(sem, this.rightOperand.`type`) && !this.rightOperand.`type`.isA(sem, this.leftOperand.`type`)) {
          ClassSymbol.typeError(this.leftOperand.`type`, this.rightOperand.`type`, this.rightOperand.position)
        }

        this.`type` = Types.boolType
    }
  }

  def print(tree: TreeStream) {
    tree.println(this.operator.toString + (if (this.`type` == null) "" else " : " + this.`type`.name))
    tree.indent
    this.leftOperand.print(tree)
    this.rightOperand.print(tree)
    tree.unindent
  }

  def generateCode(code: CodeStream) {
    /* If one of the operands is NULL, then the other one must be an object.
     * Box the value if this is the case. */
    this.leftOperand.generateCode(code, (this.leftOperand.`type` eq Types.nullType))
    this.rightOperand.generateCode(code, (this.rightOperand.`type` eq Types.nullType))

    code.println("; " + this.operator)

    code.println("MRM R5, (R2)")
    code.println("SUB R2, R1")
    code.println("MRM R6, (R2)")

    this.operator match {
      case AND =>
        code.println("AND R6, R5")

      case OR =>
        code.println("OR R6, R5")

      case PLUS =>
        code.println("ADD R6, R5")

      case MINUS =>
        code.println("SUB R6, R5")

      case MUL =>
        code.println("MUL R6, R5")

      case DIV =>
        code.println("DIV R6, R5")

      case MOD =>
        code.println("MOD R6, R5")

      case GT =>
        code.println("SUB R6, R5")
        code.println("ISP R6, R6")

      case GTEQ =>
        code.println("SUB R6, R5")
        code.println("ISN R6, R6")
        code.println("XOR R6, R1")

      case LT =>
        code.println("SUB R6, R5")
        code.println("ISN R6, R6")

      case LTEQ =>
        code.println("SUB R6, R5")
        code.println("ISP R6, R6")
        code.println("XOR R6, R1")

      case EQ =>
        code.println("SUB R6, R5")
        code.println("ISZ R6, R6")

      case NEQ =>
        code.println("SUB R6, R5")
        code.println("ISZ R6, R6")
        code.println("XOR R6, R1")
    }

    code.println("MMR (R2), R6")
  }
}