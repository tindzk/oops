package org.oopsc.statement

import org.oopsc._
import org.oopsc.expression.Expression

/**
 * Statement printing the given operand.
 */
class WriteStatement(var operand: Expression) extends Statement {
  override def refPass(sem: SemanticAnalysis) {
    this.operand.refPass(sem)
    this.operand.resolvedType.check(Types.intType, this.operand.position)
  }

  override def optimPass() : Statement = {
    this.operand = this.operand.optimPass()
    this
  }

  override def print(tree: TreeStream) {
    tree.println("WRITE")
    tree.indent
    this.operand.print(tree)
    tree.unindent
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; WRITE operand code")
    this.operand.generateCode(code, false)

    code.println("; WRITE")
    code.println("MRM R5, (R2)")
    code.println("SUB R2, R1")
    code.println("SYS 1, 5")
  }
}