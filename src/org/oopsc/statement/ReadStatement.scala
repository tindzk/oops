package org.oopsc.statement

import org.oopsc._
import org.oopsc.expression._
import org.oopsc.symbol._

/**
 * Reads a character and stores it in the given operand.
 */
class ReadStatement(var operand: Expression) extends Statement {
  /** An expression for creating a new object with the type Integer. */
  var newInt: Expression = new NewExpression(new ResolvableClassSymbol(new Identifier("Integer")))

  override def refPass(sem: SemanticAnalysis) {
    this.operand.refPass(sem)

    if (!this.operand.lValue) {
      throw new CompileException("Lvalue expected", this.operand.position)
    }

    this.operand.resolvedType.check(sem, Types.intClass, this.operand.position)
    this.newInt.refPass(sem)
  }

  override def print(tree: TreeStream) {
    tree.println("READ")
    tree.indent
    this.operand.print(tree)
    tree.unindent
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; READ lvalue ablegen")
    this.operand.generateCode(code)

    code.println("; READ Speicher allokieren")
    this.newInt.generateCode(code)

    code.println("; READ")
    code.println("MRM R5, (R2)") /* R2 points to a boxed Integer value. */

    /* Skip header. */
    code.println("MRI R6, " + ClassSymbol.HEADERSIZE)
    code.println("ADD R5, R6")

    code.println("SYS 0, 6 ; Gelesenen Wert in R6 ablegen")
    code.println("MMR (R5), R6 ; Zeichen in neuen Integer schreiben")
    code.println("MRM R5, (R2) ; Neuen Integer vom Stapel entnehmen")
    code.println("SUB R2, R1")
    code.println("MRM R6, (R2) ; Ziel vom Stapel entnehmen")
    code.println("SUB R2, R1")
    code.println("MMR (R6), R5 ; Zuweisen")
  }
}