package org.oopsc.expression

import org.oopsc._
import org.oopsc.statement._
import org.oopsc.symbol.ClassSymbol
import com.typesafe.scalalogging.slf4j.Logging

object BinaryExpression extends Enumeration {
  type Operator = Value
  val EQ, NEQ, GT, GTEQ, LT, LTEQ, PLUS, MINUS, MUL, DIV, MOD, AND, OR, NOT = Value
}

/**
 * Represents an expression with a binary operator in the syntax tree.
 */
class BinaryExpression(var leftOperand: Expression, var operator: BinaryExpression.Operator, var rightOperand: Expression) extends Expression(leftOperand.position) with Logging {
  import BinaryExpression._

  var t: ClassSymbol = null

  override def refPass(sem: SemanticAnalysis) {
    this.leftOperand.refPass(sem)
    this.rightOperand.refPass(sem)

    this.operator match {
      case AND | OR =>
        this.leftOperand.resolvedType().check(Types.boolType, this.leftOperand.position)
        this.rightOperand.resolvedType().check(Types.boolType, this.rightOperand.position)
        this.t = Types.boolType

      case PLUS | MINUS | MUL | DIV | MOD =>
        this.leftOperand.resolvedType().check(Types.intType, this.leftOperand.position)
        this.rightOperand.resolvedType().check(Types.intType, this.rightOperand.position)
        this.t = Types.intType

      case GT | GTEQ | LT | LTEQ =>
        this.leftOperand.resolvedType().check(Types.intType, this.leftOperand.position)
        this.rightOperand.resolvedType().check(Types.intType, this.rightOperand.position)
        this.t = Types.boolType

      case EQ | NEQ =>
        if (!this.leftOperand.resolvedType().isA(this.rightOperand.resolvedType()) && !this.rightOperand.resolvedType().isA(this.leftOperand.resolvedType())) {
          ClassSymbol.typeError(this.leftOperand.resolvedType(), this.rightOperand.resolvedType(), this.rightOperand.position)
        }

        this.t = Types.boolType
    }
  }

  override def optimPass(): Expression = {
    this.leftOperand = this.leftOperand.optimPass()
    this.rightOperand = this.rightOperand.optimPass()

    (this.leftOperand, this.rightOperand, this.operator) match {
      case (IntegerLiteralExpression(0, _), r: Expression, PLUS) =>
        return r
      case (IntegerLiteralExpression(0, _), r: Expression, MINUS) =>
        return new UnaryExpression(UnaryExpression.MINUS, r, this.position)
      case (IntegerLiteralExpression(0, _), r: Expression, MUL) =>
        logger.warn(s"${this.position}: Expression short-circuits to zero. The right operand is never evaluated.")
        return IntegerLiteralExpression(0, this.position)
      case (IntegerLiteralExpression(0, _), r: Expression, DIV) =>
        return IntegerLiteralExpression(0, this.position)
      case (IntegerLiteralExpression(1, _), r: Expression, MUL) =>
        return r
      case (l: Expression, IntegerLiteralExpression(1, _), MUL) =>
        return l
      case (l: Expression, IntegerLiteralExpression(1, _), DIV) =>
        return l
      case (l: Expression, IntegerLiteralExpression(0, _), PLUS) =>
        return l
      case (l: Expression, IntegerLiteralExpression(0, _), MINUS) =>
        return l
      case (l: Expression, IntegerLiteralExpression(0, _), MUL) =>
        logger.warn(s"${this.position}: Expression short-circuits to zero. The left operand is never evaluated.")
        return IntegerLiteralExpression(0, this.position)
      case (l: Expression, IntegerLiteralExpression(0, _), DIV) =>
        throw new CompileException("Division by zero.", this.position)
      case (l: BooleanLiteralExpression, r: BooleanLiteralExpression, AND) =>
        val value = l.value && r.value
        return BooleanLiteralExpression(value, this.position)
      case (l: BooleanLiteralExpression, r: BooleanLiteralExpression, OR) =>
        val value = l.value || r.value
        return BooleanLiteralExpression(value, this.position)
      case (l: BooleanLiteralExpression, r: BooleanLiteralExpression, EQ) =>
        val value = l.value == r.value
        return BooleanLiteralExpression(value, this.position)
      case (l: BooleanLiteralExpression, r: BooleanLiteralExpression, NEQ) =>
        val value = l.value != r.value
        return BooleanLiteralExpression(value, this.position)
      case (l: Expression, r@BooleanLiteralExpression(false, _), AND) =>
        return r
      case (l: Expression, r@BooleanLiteralExpression(false, _), OR) =>
        return l
      case (l: Expression, r@BooleanLiteralExpression(true, _), AND) =>
        return l
      case (l: Expression, r@BooleanLiteralExpression(true, _), OR) =>
        return r
      case (l@BooleanLiteralExpression(false, _), r: Expression, AND) =>
        return l
      case (l@BooleanLiteralExpression(false, _), r: Expression, OR) =>
        return r
      case (l@BooleanLiteralExpression(true, _), r: Expression, AND) =>
        return r
      case (l@BooleanLiteralExpression(true, _), r: Expression, OR) =>
        return l
      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression, PLUS) =>
        val value = l.value + r.value
        return IntegerLiteralExpression(value, this.position)
      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression, MINUS) =>
        val value = l.value - r.value
        return IntegerLiteralExpression(value, this.position)
      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression, MUL) =>
        val value = l.value * r.value
        return IntegerLiteralExpression(value, this.position)
      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression, DIV) =>
        if (r.value == 0) {
          throw new CompileException("Division by zero.", this.position)
        }
        val value = l.value / r.value
        return IntegerLiteralExpression(value, this.position)
      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression, MOD) =>
        val value = l.value % r.value
        return IntegerLiteralExpression(value, this.position)
      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression, GT) =>
        val value = l.value > r.value
        return BooleanLiteralExpression(value, this.position)
      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression, GTEQ) =>
        val value = l.value >= r.value
        return BooleanLiteralExpression(value, this.position)
      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression, LT) =>
        val value = l.value < r.value
        return BooleanLiteralExpression(value, this.position)
      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression, LTEQ) =>
        val value = l.value <= r.value
        return BooleanLiteralExpression(value, this.position)
      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression, EQ) =>
        val value = l.value == r.value
        return BooleanLiteralExpression(value, this.position)
      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression, NEQ) =>
        val value = l.value != r.value
        return BooleanLiteralExpression(value, this.position)
      case (l@IntegerLiteralExpression(value, _), r@UnaryExpression(UnaryExpression.MINUS, _, _), MUL) =>
          /* c * (-x) → -c * x */
          l.value = -l.value
          this.rightOperand = r.operand
      case (l@IntegerLiteralExpression(value, _), r@UnaryExpression(UnaryExpression.MINUS, _, _), DIV) =>
        /* c / -x → -c / x */
        l.value = -l.value
        this.rightOperand = r.operand
      case (l@UnaryExpression(UnaryExpression.MINUS, _, _), r@IntegerLiteralExpression(value, _), MUL) =>
        /* -x * c → x * (-c) */
        r.value = -r.value
        this.leftOperand = l.operand
      case (l@UnaryExpression(UnaryExpression.MINUS, _, _), r@IntegerLiteralExpression(value, _), DIV) =>
        /* -x / c → x / (-c) */
        r.value = -r.value
        this.leftOperand = l.operand
      case (l@UnaryExpression(UnaryExpression.MINUS, _, _), r@UnaryExpression(UnaryExpression.MINUS, _, _), MUL) =>
        /* -x * (-y) → x * y */
        return new BinaryExpression(l.operand, BinaryExpression.MUL, r.operand)
      case (l@UnaryExpression(UnaryExpression.MINUS, _, _), r@UnaryExpression(UnaryExpression.MINUS, _, _), DIV) =>
        /* -x / (-y) → x / y */
        return new BinaryExpression(l.operand, BinaryExpression.DIV, r.operand)
      case (l@UnaryExpression(UnaryExpression.MINUS, _, _), r: Expression, MUL) =>
        /* -x * y → -(x * y) */
        return new UnaryExpression(UnaryExpression.MINUS,
          new BinaryExpression(l.operand, BinaryExpression.MUL, r), this.position)
      case (l@UnaryExpression(UnaryExpression.MINUS, _, _), r: Expression, DIV) =>
        /* -x / y → -(x / y) */
        return new UnaryExpression(UnaryExpression.MINUS,
          new BinaryExpression(l.operand, BinaryExpression.DIV, r), this.position)
      case (l: Expression, r@UnaryExpression(UnaryExpression.MINUS, _, _), MUL) =>
        /* x * (-y) → -(x * y) */
        return new UnaryExpression(UnaryExpression.MINUS,
          new BinaryExpression(l, BinaryExpression.MUL, r.operand), this.position)
      case (l: Expression, r@UnaryExpression(UnaryExpression.MINUS, _, _), DIV) =>
        /* x / (-y) → -(x / y) */
        return new UnaryExpression(UnaryExpression.MINUS,
          new BinaryExpression(l, BinaryExpression.DIV, r.operand), this.position)
      case (l: Expression, r@UnaryExpression(UnaryExpression.MINUS, _, _), PLUS) =>
        /* x + (-y) → x - y */
        this.operator = MINUS
        this.rightOperand = r.operand
      case (l: Expression, r@UnaryExpression(UnaryExpression.MINUS, _, _), MINUS) =>
        /* x - (-y) → x + y */
        this.operator = PLUS
        this.rightOperand = r.operand

      case _ =>
    }

    return this
  }

  override def resolvedType() = t

  def print(tree: TreeStream) {
    tree.println(this.operator.toString + " : " + this.resolvedType().name)
    tree.indent
    this.leftOperand.print(tree)
    this.rightOperand.print(tree)
    tree.unindent
  }

  def generateCode(code: CodeStream) {
    /* If one of the operands is NULL, then the other one must be an object.
     * Box the value if this is the case. */
    this.leftOperand.generateCode(code, (this.leftOperand.resolvedType() eq Types.nullType))
    this.rightOperand.generateCode(code, (this.rightOperand.resolvedType() eq Types.nullType))

    code.println("; " + this.operator)

    code.println("MRM R5, (R2)")

    if (this.operator == DIV) {
      /* For the DIV operator, throw an exception if the right operand is zero. */
      val nextLabel = code.nextLabel
      code.println("JPC R5, " + nextLabel)
      new ThrowStatement(new IntegerLiteralExpression(0)).generateCode(code)
      code.println(nextLabel + ":")
    }

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