package org.oopsc.statement

import org.oopsc._
import org.oopsc.expression._
import org.oopsc.symbol._

/**
 * Reads a character and stores it in the given operand.
 */
class ReadStatement(var operand: Expression) extends Statement {
  /** An expression for creating a new object with the type Integer. */
  private var newInt = new NewExpression(new ResolvableClassSymbol(new Identifier("Integer")))

  override def refPass(sem: SemanticAnalysis) {
    this.operand.refPass(sem)

    if (!this.operand.lValue) {
      throw new CompileException("Variable reference expected.", this.operand.position)
    }

    this.operand.resolvedType.check(Types.intClass, this.operand.position)
    this.newInt.refPass(sem)
  }

  override def optimPass() : Statement = {
    this.operand = this.operand.optimPass()
    this
  }

  override def print(tree: TreeStream) {
    tree.println("READ")
    tree.indent
    this.operand.print(tree)
    tree.unindent
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; READ")
    code.println("; Push operand (lvalue) on the stack.")
    this.operand.generateCode(code)

    code.println("; Allocate memory for the character.")
    this.newInt.generateCode(code)

    code.println("; READ")
    code.println("MRM R5, (R2)") /* R2 points to a boxed Integer value. */

    /* Skip header. */
    code.println(s"MRI R6, ${ClassSymbol.HEADERSIZE}")
    code.println("ADD R5, R6")

    code.println("SYS 0, 6 ; Store read value in R6.")
    code.println("MMR (R5), R6 ; Set the value of the Integer object to the read character.")
    code.println("MRM R5, (R2) ; Read the allocated object reference from the stack.")
    code.println("SUB R2, R1")
    code.println("MRM R6, (R2) ; Read destination from the stack.")
    code.println("SUB R2, R1")
    code.println("MMR (R6), R5 ; Assign.")
  }
}