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

  var t: ClassSymbol = null

  override def refPass(sem: SemanticAnalysis) {
    this.leftOperand.refPass(sem)
    this.rightOperand.refPass(sem)

    this.operator match {
      case AND | OR =>
        this.leftOperand.resolvedType().check(sem, Types.boolType, this.leftOperand.position)
        this.rightOperand.resolvedType().check(sem, Types.boolType, this.rightOperand.position)
        this.t = Types.boolType

      case PLUS | MINUS | MUL | DIV | MOD =>
        this.leftOperand.resolvedType().check(sem, Types.intType, this.leftOperand.position)
        this.rightOperand.resolvedType().check(sem, Types.intType, this.rightOperand.position)
        this.t = Types.intType

      case GT | GTEQ | LT | LTEQ =>
        this.leftOperand.resolvedType().check(sem, Types.intType, this.leftOperand.position)
        this.rightOperand.resolvedType().check(sem, Types.intType, this.rightOperand.position)
        this.t = Types.boolType

      case EQ | NEQ =>
        if (!this.leftOperand.resolvedType().isA(sem, this.rightOperand.resolvedType()) && !this.rightOperand.resolvedType().isA(sem, this.leftOperand.resolvedType())) {
          ClassSymbol.typeError(this.leftOperand.resolvedType(), this.rightOperand.resolvedType(), this.rightOperand.position)
        }

        this.t = Types.boolType
    }
  }

  override def optimPass() : Expression = {
    /* TODO Better handling for EQ, NEQ. */

    this.leftOperand = this.leftOperand.optimPass()
    this.rightOperand = this.rightOperand.optimPass()

    (this.leftOperand, this.rightOperand) match {
      case (l: BooleanLiteralExpression, r: BooleanLiteralExpression) =>
        this.operator match {
          case AND =>
            val value = l.value && r.value
            return BooleanLiteralExpression(value, this.position)
          case OR =>
            val value = l.value || r.value
            return BooleanLiteralExpression(value, this.position)
          case EQ =>
            val value = l.value == r.value
            return BooleanLiteralExpression(value, this.position)
          case NEQ =>
            val value = l.value != r.value
            return BooleanLiteralExpression(value, this.position)
        }

      case (l: Expression, r @ BooleanLiteralExpression(false, _)) =>
        if (this.operator == AND) {
          return r
        } else if (this.operator == OR) {
          return l
        }

      case (l: Expression, r @ BooleanLiteralExpression(true, _)) =>
        if (this.operator == AND) {
          return l
        } else if (this.operator == OR) {
          return r
        }

      case (l @ BooleanLiteralExpression(false, _), r: Expression) =>
        if (this.operator == AND) {
          return l
        } else if (this.operator == OR) {
          return r
        }

      case (l @ BooleanLiteralExpression(true, _), r: Expression) =>
        if (this.operator == AND) {
          return r
        } else if (this.operator == OR) {
          return l
        }

      case (l: IntegerLiteralExpression, r: IntegerLiteralExpression) =>
        this.operator match {
          case PLUS =>
            val value = l.value + r.value
            return IntegerLiteralExpression(value, this.position)
          case MINUS =>
            val value = l.value - r.value
            return IntegerLiteralExpression(value, this.position)
          case MUL =>
            val value = l.value * r.value
            return IntegerLiteralExpression(value, this.position)
          case DIV =>
            if (r.value == 0) {
              throw new CompileException("Division by zero.", this.position)
            }
            val value = l.value / r.value
            return IntegerLiteralExpression(value, this.position)
          case MOD =>
            val value = l.value % r.value
            return IntegerLiteralExpression(value, this.position)
          case GT =>
            val value = l.value > r.value
            return BooleanLiteralExpression(value, this.position)
          case GTEQ =>
            val value = l.value >= r.value
            return BooleanLiteralExpression(value, this.position)
          case LT =>
            val value = l.value < r.value
            return BooleanLiteralExpression(value, this.position)
          case LTEQ =>
            val value = l.value <= r.value
            return BooleanLiteralExpression(value, this.position)
          case EQ =>
            val value = l.value == r.value
            return BooleanLiteralExpression(value, this.position)
          case NEQ =>
            val value = l.value != r.value
            return BooleanLiteralExpression(value, this.position)
        }

      case (IntegerLiteralExpression(0, _), r: Expression) =>
        this.operator match {
          case PLUS =>
            return r
          case MINUS =>
            return new UnaryExpression(UnaryExpression.MINUS, r, this.position)
          case MUL =>
            /* TODO Use logging library. */
            println("Warning: Expression short-circuits to zero. The right operand is never evaluated.")
            return IntegerLiteralExpression(0, this.position)
          case DIV =>
            return IntegerLiteralExpression(0, this.position)
          case _ =>
        }

      case (IntegerLiteralExpression(1, _), r: Expression) =>
        this.operator match {
          case MUL =>
            return r
          case _ =>
        }

      case (l: Expression, IntegerLiteralExpression(1, _)) =>
        this.operator match {
          case MUL =>
            return l
          case DIV =>
            return l
          case _ =>
        }

      case (l: Expression, IntegerLiteralExpression(0, _)) =>
        this.operator match {
          case PLUS =>
            return l
          case MINUS =>
            return l
          case MUL =>
            println("Warning: Expression short-circuits to zero. The left operand is never evaluated.")
            return IntegerLiteralExpression(0, this.position)
          case DIV =>
            throw new CompileException("Division by zero.", this.position)
          case _ =>
        }

      case (l @ IntegerLiteralExpression(value, _), r: UnaryExpression) =>
        /* c * (-x) → -c * x */
        if (this.operator == MUL && r.operator == UnaryExpression.MINUS) {
          l.value = -l.value
          this.rightOperand = r.operand
        } else if (this.operator == DIV && r.operator == UnaryExpression.MINUS) {
          /* c / -x → -c / x */
          l.value = -l.value
          this.rightOperand = r.operand
        }

      case (l: UnaryExpression, r @ IntegerLiteralExpression(value, _)) =>
        if (this.operator == MUL && l.operator == UnaryExpression.MINUS) {
          /* -x * c → x * (-c) */
          r.value = -r.value
          this.leftOperand = l.operand
        } else if (this.operator == DIV && l.operator == UnaryExpression.MINUS) {
          /* -x / c → x / (-c) */
          r.value = -r.value
          this.leftOperand = l.operand
        }

      case (l: UnaryExpression, r: UnaryExpression) =>
        if (this.operator == MUL && l.operator == r.operator == UnaryExpression.MINUS) {
          /* -x * (-y) → x * y */
          return new BinaryExpression(l.operand, BinaryExpression.MUL, r.operand)
        } else if (this.operator == DIV && l.operator == r.operator == UnaryExpression.MINUS) {
          /* -x / (-y) → x / y */
          return new BinaryExpression(l.operand, BinaryExpression.DIV, r.operand)
        }

      case (l: UnaryExpression, r: Expression) =>
        if (this.operator == MUL && l.operator == UnaryExpression.MINUS) {
          /* -x * y → -(x * y) */
          return new UnaryExpression(UnaryExpression.MINUS,
            new BinaryExpression(l.operand, BinaryExpression.MUL, r), this.position)
        } else if (this.operator == DIV && l.operator == UnaryExpression.MINUS) {
          /* -x / y → -(x / y) */
          return new UnaryExpression(UnaryExpression.MINUS,
            new BinaryExpression(l.operand, BinaryExpression.DIV, r), this.position)
        }

      case (l: Expression, r: UnaryExpression) =>
        if (this.operator == MUL && r.operator == UnaryExpression.MINUS) {
          /* x * (-y) → -(x * y) */
          return new UnaryExpression(UnaryExpression.MINUS,
            new BinaryExpression(l, BinaryExpression.MUL, r.operand), this.position)
        } else if (this.operator == DIV && r.operator == UnaryExpression.MINUS) {
          /* x / (-y) → -(x / y) */
          return new UnaryExpression(UnaryExpression.MINUS,
            new BinaryExpression(l, BinaryExpression.DIV, r.operand), this.position)
        }

      case (l: Expression, r: UnaryExpression) =>
        if (this.operator == PLUS && r.operator == UnaryExpression.MINUS) {
          /* x + (-y) → x - y */
          this.operator = MINUS
          this.rightOperand = r.operand
        } else if (this.operator == MINUS && r.operator == UnaryExpression.MINUS) {
          /* x - (-y) → x + y */
          this.operator = PLUS
          this.rightOperand = r.operand
        }

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